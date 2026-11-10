package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service("servicioRecordatorio")
@Transactional
public class ServicioRecordatorioImpl implements ServicioRecordatorio {

  private static final Logger LOGGER = Logger.getLogger(ServicioRecordatorioImpl.class.getName());
  private static final int HORAS_DE_ANTICIPACION = 24;

  private final RepositorioTurno repositorioTurno;
  private final ServicioNotificacion servicioNotificacion;
  private final Clock reloj;

  @Autowired
  public ServicioRecordatorioImpl(
    RepositorioTurno repositorioTurno,
    ServicioNotificacion servicioNotificacion
  ) {
    this(repositorioTurno, servicioNotificacion, Clock.systemDefaultZone());
  }

  public ServicioRecordatorioImpl(
    RepositorioTurno repositorioTurno,
    ServicioNotificacion servicioNotificacion,
    Clock reloj
  ) {
    this.repositorioTurno = repositorioTurno;
    this.servicioNotificacion = servicioNotificacion;
    this.reloj = reloj;
  }

  @Override
  @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
  public void enviarRecordatorios() {
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDateTime limite = ahora.plusHours(HORAS_DE_ANTICIPACION);
    List<Turno> candidatos = repositorioTurno.listarConfirmadosSinRecordatorio(
      ahora.toLocalDate(),
      limite.toLocalDate()
    );
    for (Turno turno : candidatos) {
      LocalDateTime momento = turno.getFecha().atTime(turno.getHora());
      if (momento.isAfter(ahora) && !momento.isAfter(limite)) {
        recordar(turno);
      }
    }
  }

  private void recordar(Turno turno) {
    try {
      Usuario usuario = turno.getMascota().getSocio().getUsuario();
      String email = usuario == null ? null : usuario.getEmail();
      if (email == null || email.isBlank()) {
        return;
      }
      servicioNotificacion.enviarRecordatorioTurno(turno, email);
      turno.marcarRecordatorioEnviado();
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(
          Level.WARNING,
          "No se pudo enviar el recordatorio del turno " + turno.getId(),
          e
        );
      }
    }
  }
}
