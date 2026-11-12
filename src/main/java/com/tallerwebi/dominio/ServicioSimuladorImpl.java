package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    List<ItemAtencion> items = new ArrayList<>();
    for (Turno turno : turnos) {
      items.add(armarItem(turno, planes));
    }
    List<ResultadoPlan> resultados = new ArrayList<>();
    for (Plan plan : planes) {
      resultados.add(armarResultado(plan, mascota, meses, turnos));
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

  private ItemAtencion armarItem(Turno turno, List<Plan> planes) {
    Set<Long> cubren = new HashSet<>();
    for (Plan plan : planes) {
      if (plan.cubre(turno.getServicio())) {
        cubren.add(plan.getId());
      }
    }
    return new ItemAtencion(
      turno.getFecha(),
      turno.getServicio().getNombre(),
      turno.getPrecioParticular(),
      cubren
    );
  }

  private ResultadoPlan armarResultado(Plan plan, Mascota mascota, int meses, List<Turno> turnos) {
    double noCubierto = 0;
    for (Turno turno : turnos) {
      if (!plan.cubre(turno.getServicio())) {
        noCubierto += turno.getPrecioParticular();
      }
    }
    Plan planActual = mascota.getPlan();
    boolean actual = planActual != null && Objects.equals(planActual.getId(), plan.getId());
    return new ResultadoPlan(plan, meses, noCubierto, actual);
  }
}
