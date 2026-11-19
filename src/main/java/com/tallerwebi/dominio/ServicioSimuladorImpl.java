package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioSimulador")
@Transactional
public class ServicioSimuladorImpl implements ServicioSimulador {

  private final ServicioMascota servicioMascota;
  private final ServicioTurno servicioTurno;
  private final RepositorioPlan repositorioPlan;
  private final Clock reloj;

  @Autowired
  public ServicioSimuladorImpl(
    ServicioMascota servicioMascota,
    ServicioTurno servicioTurno,
    RepositorioPlan repositorioPlan
  ) {
    this(servicioMascota, servicioTurno, repositorioPlan, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests.
  public ServicioSimuladorImpl(
    ServicioMascota servicioMascota,
    ServicioTurno servicioTurno,
    RepositorioPlan repositorioPlan,
    Clock reloj
  ) {
    this.servicioMascota = servicioMascota;
    this.servicioTurno = servicioTurno;
    this.repositorioPlan = repositorioPlan;
    this.reloj = reloj;
  }

  @Override
  public SimulacionPlan simular(Socio socio, Long mascotaId, int meses) throws TurnoInvalido {
    Mascota mascota = servicioMascota.buscarPorIdParaSocio(socio, mascotaId);
    if (mascota == null) {
      return null;
    }
    List<Plan> planes = repositorioPlan.listar();
    List<Turno> turnos = atencionesConPrecio(socio, mascotaId, meses);

    List<ItemAtencion> items = armarItems(turnos, planes);
    List<ResultadoPlan> resultados = new ArrayList<>();
    for (Plan plan : planes) {
      resultados.add(armarResultado(plan, mascota, meses, items));
    }
    return new SimulacionPlan(mascota, meses, resultados, items);
  }

  /** Solo cuentan las consultas finalizadas, con precio cargado y dentro del período. */
  private List<Turno> atencionesConPrecio(Socio socio, Long mascotaId, int meses)
    throws TurnoInvalido {
    LocalDate desde = LocalDate.now(reloj).minusMonths(meses);
    List<Turno> pasados = servicioTurno.historialDe(socio, mascotaId).getPasados();
    List<Turno> elegidas = new ArrayList<>();
    for (Turno turno : pasados) {
      boolean finalizada = turno.getEstado() == EstadoTurno.FINALIZADO;
      boolean conPrecio = turno.getPrecioParticular() != null;
      if (finalizada && conPrecio && !turno.getFecha().isBefore(desde)) {
        elegidas.add(turno);
      }
    }
    elegidas.sort(Comparator.comparing(Turno::getFecha));
    return elegidas;
  }

  /** Para cada atención calcula lo que se paga con cada plan. Van en orden de fecha. */
  private List<ItemAtencion> armarItems(List<Turno> turnos, List<Plan> planes) {
    Map<Long, Map<YearMonth, Integer>> cubiertasPorMes = new HashMap<>();
    List<ItemAtencion> items = new ArrayList<>();
    for (Turno turno : turnos) {
      Map<Long, Double> pagoPorPlan = new HashMap<>();
      for (Plan plan : planes) {
        pagoPorPlan.put(plan.getId(), pagoCon(plan, turno, cubiertasPorMes));
      }
      items.add(
        new ItemAtencion(
          turno.getFecha(),
          turno.getServicio().getNombre(),
          turno.getPrecioParticular(),
          pagoPorPlan
        )
      );
    }
    return items;
  }

  /**
   * Lo que el dueño paga por una atención con un plan:
   * si el plan no la cubre o ya pasó el tope del mes, el precio particular completo;
   * si la cubre dentro del tope, solo el copago (nunca más que el precio).
   */
  private double pagoCon(Plan plan, Turno turno, Map<Long, Map<YearMonth, Integer>> cubiertas) {
    double precio = turno.getPrecioParticular();
    if (!plan.cubre(turno.getServicio())) {
      return precio;
    }
    Map<YearMonth, Integer> delPlan = cubiertas.computeIfAbsent(
      plan.getId(),
      id -> new HashMap<>()
    );
    int numeroEnElMes = delPlan.merge(YearMonth.from(turno.getFecha()), 1, Integer::sum);
    if (!plan.dentroDelTope(numeroEnElMes)) {
      return precio;
    }
    return Math.min(precio, plan.copagoPorAtencion());
  }

  private ResultadoPlan armarResultado(
    Plan plan,
    Mascota mascota,
    int meses,
    List<ItemAtencion> items
  ) {
    double pagoAparte = 0;
    for (ItemAtencion item : items) {
      pagoAparte += item.pagoCon(plan.getId());
    }
    Plan planActual = mascota.getPlan();
    boolean actual = planActual != null && Objects.equals(planActual.getId(), plan.getId());
    return new ResultadoPlan(plan, meses, pagoAparte, actual);
  }
}
