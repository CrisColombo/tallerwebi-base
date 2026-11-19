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

  /**
   * Cada servicio del turno (el principal y los extras) es una atención propia.
   * Van en orden de fecha, y dentro de un turno primero el principal.
   */
  private List<ItemAtencion> armarItems(List<Turno> turnos, List<Plan> planes) {
    Map<Long, Map<YearMonth, CobroMensual>> cobros = new HashMap<>();
    List<ItemAtencion> items = new ArrayList<>();
    for (Turno turno : turnos) {
      items.add(
        armarItem(
          turno.getFecha(),
          turno.getServicio(),
          turno.getPrecioParticular(),
          planes,
          cobros
        )
      );
      for (AtencionAdicional extra : turno.getAdicionales()) {
        items.add(
          armarItem(turno.getFecha(), extra.getServicio(), extra.getPrecio(), planes, cobros)
        );
      }
    }
    return items;
  }

  /** Lo que se paga por una atención con cada plan, según tope y copago del mes. */
  private ItemAtencion armarItem(
    LocalDate fecha,
    Servicio servicio,
    double precio,
    List<Plan> planes,
    Map<Long, Map<YearMonth, CobroMensual>> cobros
  ) {
    Map<Long, Double> pagoPorPlan = new HashMap<>();
    for (Plan plan : planes) {
      CobroMensual cobro = cobros
        .computeIfAbsent(plan.getId(), id -> new HashMap<>())
        .computeIfAbsent(YearMonth.from(fecha), mes -> new CobroMensual(plan, 0));
      pagoPorPlan.put(plan.getId(), cobro.cobrar(servicio, precio));
    }
    return new ItemAtencion(fecha, servicio.getNombre(), precio, pagoPorPlan);
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
