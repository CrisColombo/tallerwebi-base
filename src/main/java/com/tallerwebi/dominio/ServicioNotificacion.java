package com.tallerwebi.dominio;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioNotificacion {
  void enviarConfirmacionTurno(Turno turno, String emailDestino);
}
