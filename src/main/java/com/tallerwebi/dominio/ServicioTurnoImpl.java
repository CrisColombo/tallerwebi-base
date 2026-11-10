package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioTurno")
@Transactional
public class ServicioTurnoImpl implements ServicioTurno {

  private static final Logger LOGGER = Logger.getLogger(ServicioTurnoImpl.class.getName());

  private final RepositorioTurno repositorioTurno;
  private final RepositorioVeterinaria repositorioVeterinaria;
  private final RepositorioServicio repositorioServicio;
  private final ServicioNotificacion servicioNotificacion;
  private final Clock reloj;

  @Autowired
  public ServicioTurnoImpl(
      RepositorioTurno repositorioTurno,
      RepositorioVeterinaria repositorioVeterinaria,
      RepositorioServicio repositorioServicio,
      ServicioNotificacion servicioNotificacion) {
    this(
        repositorioTurno,
        repositorioVeterinaria,
        repositorioServicio,
        servicioNotificacion,
        Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests.
  public ServicioTurnoImpl(
      RepositorioTurno repositorioTurno,
      RepositorioVeterinaria repositorioVeterinaria,
      RepositorioServicio repositorioServicio,
      ServicioNotificacion servicioNotificacion,
      Clock reloj) {
    this.repositorioTurno = repositorioTurno;
    this.repositorioVeterinaria = repositorioVeterinaria;
    this.repositorioServicio = repositorioServicio;
    this.servicioNotificacion = servicioNotificacion;
    this.reloj = reloj;
  }

  @Override
  public List<LocalTime> horariosDisponibles(Veterinaria veterinaria, LocalDate fecha) {
    LocalDate hoy = LocalDate.now(reloj);
    if (veterinaria == null || fecha == null || fecha.isBefore(hoy) || !veterinaria.atiendeEl(fecha)) {
      return List.of();
    }
    List<LocalTime> ocupadas = repositorioTurno.horasOcupadas(veterinaria.getId(), fecha);
    LocalTime ahora = LocalTime.now(reloj);
    return veterinaria
        .horariosDeAtencion()
        .stream()
        .filter(h -> !ocupadas.contains(h))
        .filter(h -> fecha.isAfter(hoy) || h.isAfter(ahora))
        .collect(Collectors.toList());
  }

  @Override
  public Turno reservar(Socio socio, SolicitudTurno solicitud) throws TurnoInvalido {
    Mascota mascota = socio.buscarMascota(solicitud.getMascotaId());
    if (mascota == null) {
      throw new TurnoInvalido("Elegí una de tus mascotas");
    }
    Veterinaria veterinaria = repositorioVeterinaria.buscarPorId(solicitud.getVeterinariaId());
    Servicio servicio = repositorioServicio.buscarPorId(solicitud.getServicioId());
    validarServicio(mascota, veterinaria, servicio);
    validarHorario(veterinaria, solicitud.getFecha(), solicitud.getHora());

    Turno turno = new Turno();
    turno.setMascota(mascota);
    turno.setVeterinaria(veterinaria);
    turno.setServicio(servicio);
    turno.setFecha(solicitud.getFecha());
    turno.setHora(solicitud.getHora());
    turno.setObservaciones(solicitud.getObservaciones());
    repositorioTurno.guardar(turno);
    notificarConfirmacion(socio, turno);
    return turno;
  }

  private void notificarConfirmacion(Socio socio, Turno turno) {
    try {
      Usuario usuario = socio.getUsuario();
      String email = usuario == null ? null : usuario.getEmail();
      if (email == null || email.isBlank()) {
        return;
      }
      servicioNotificacion.enviarConfirmacionTurno(turno, email);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "No se pudo enviar el mail de confirmación del turno", e);
    }
  }

  private void validarServicio(Mascota mascota, Veterinaria veterinaria, Servicio servicio)
      throws TurnoInvalido {
    if (veterinaria == null || servicio == null) {
      throw new TurnoInvalido("Elegí una veterinaria y un servicio");
    }
    exigirPlanVigente(mascota);
    if (!veterinaria.ofrece(servicio)) {
      throw new TurnoInvalido(veterinaria.getNombre() + " no ofrece " + servicio.getNombre());
    }
    if (!mascota.tieneCubierto(servicio)) {
      throw new TurnoInvalido(
          servicio.getNombre() + " no está incluido en el plan de " + mascota.getNombre());
    }
  }

  private void exigirPlanVigente(Mascota mascota) throws TurnoInvalido {
    if (mascota.getPlan() == null || !mascota.suscripcionAlDia()) {
      throw new TurnoInvalido(
          "Tu mascota no tiene un plan vigente. Pagá la suscripción para sacar un turno.");
    }
  }

  private void validarHorario(Veterinaria veterinaria, LocalDate fecha, LocalTime hora)
      throws TurnoInvalido {
    if (fecha == null || hora == null) {
      throw new TurnoInvalido("Elegí fecha y horario");
    }
    if (!horariosDisponibles(veterinaria, fecha).contains(hora)) {
      throw new TurnoInvalido("El horario elegido ya no está disponible");
    }
  }

  @Override
  public List<Turno> turnosDe(Socio socio) {
    return repositorioTurno.listarPorSocio(socio.getId());
  }

  @Override
  public List<Turno> turnosDe(Veterinaria veterinaria) {
    return repositorioTurno.listarPorVeterinaria(veterinaria.getId());
  }

  @Override
  public void cancelar(Socio socio, Long turnoId) throws TurnoInvalido {
    Turno turno = buscarTurnoDe(socio, turnoId);
    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden cancelar turnos confirmados");
    }
    turno.cancelar();
  }

  @Override
  public void cancelarPorVeterinaria(Veterinaria veterinaria, Long turnoId) throws TurnoInvalido {
    Turno turno = buscarTurnoDeVeterinaria(veterinaria, turnoId);
    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden cancelar turnos confirmados");
    }
    turno.cancelar();
  }

  @Override
  public void reprogramar(Socio socio, Long turnoId, LocalDate diaNuevo, LocalTime horaNueva)
      throws TurnoInvalido {
    Turno turno = buscarTurnoDe(socio, turnoId);
    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden reprogramar turnos confirmados");
    }

    Veterinaria veterinaria = turno.getVeterinaria();
    validarHorario(veterinaria, diaNuevo, horaNueva);

    turno.reprogramar(diaNuevo, horaNueva);
  }

  @Override
  public void reprogramarPorVeterinaria(
      Veterinaria veterinaria,
      Long turnoId,
      LocalDate diaNuevo,
      LocalTime horaNueva) throws TurnoInvalido {
    Turno turno = buscarTurnoDeVeterinaria(veterinaria, turnoId);
    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden reprogramar turnos confirmados");
    }
    validarHorario(veterinaria, diaNuevo, horaNueva);
    turno.reprogramar(diaNuevo, horaNueva);
  }

  @Override
  public Turno buscarTurnoDeVeterinaria(Veterinaria veterinaria, Long turnoId)
      throws TurnoInvalido {
    Turno turno = turnoId == null ? null : repositorioTurno.buscarPorId(turnoId);
    if (veterinaria == null ||
        turno == null ||
        turno.getVeterinaria() == null ||
        !Objects.equals(veterinaria.getId(), turno.getVeterinaria().getId())) {
      throw new TurnoInvalido("No encontramos ese turno");
    }
    return turno;
  }

  @Override
  public HistorialTurnos historialDe(Socio socio, Long mascotaId) throws TurnoInvalido {
    if (socio.buscarMascota(mascotaId) == null) {
      throw new TurnoInvalido("No encontramos esa mascota");
    }
    List<Turno> turnos = repositorioTurno.listarPorMascota(mascotaId);
    LocalDateTime ahora = LocalDateTime.now(reloj);
    Comparator<Turno> porMomento = Comparator.comparing(this::momento);

    List<Turno> proximos = turnos
        .stream()
        .filter(t -> esProximo(t, ahora))
        .sorted(porMomento)
        .collect(Collectors.toList());
    List<Turno> pasados = turnos
        .stream()
        .filter(t -> !esProximo(t, ahora))
        .sorted(porMomento.reversed())
        .collect(Collectors.toList());
    return new HistorialTurnos(proximos, pasados);
  }

  private boolean esProximo(Turno turno, LocalDateTime ahora) {
    return turno.estaConfirmado() && momento(turno).isAfter(ahora);
  }

  private LocalDateTime momento(Turno turno) {
    return turno.getFecha().atTime(turno.getHora());
  }

  @Override
  public Turno buscarTurnoDe(Socio socio, Long turnoId) throws TurnoInvalido {
    Turno turno = turnoId == null ? null : repositorioTurno.buscarPorId(turnoId);
    if (turno == null || socio.buscarMascota(turno.getMascota().getId()) == null) {
      throw new TurnoInvalido("No encontramos ese turno");
    }
    return turno;
  }

  @Override
  public void finalizarTurno(Veterinaria veterinaria, Long turnoId) throws TurnoInvalido {
    Turno turno = buscarTurnoDeVeterinaria(veterinaria, turnoId);

    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden finalizar turnos confirmados");
    }
    turno.finalizar();
  }
}
