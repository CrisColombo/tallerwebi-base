package com.tallerwebi.dominio;

public interface ServicioNotificacion {
  void enviarConfirmacionTurno(Turno turno, String emailDestino);
  void enviarCancelacionTurno(Turno turno, String emailDestino);
  void enviarRecordatorioTurno(Turno turno, String emailDestino);
}
