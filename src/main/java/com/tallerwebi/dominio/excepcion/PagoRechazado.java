package com.tallerwebi.dominio.excepcion;

public class PagoRechazado extends Exception {

  private static final long serialVersionUID = 1L;

  public PagoRechazado(String mensaje) {
    super(mensaje);
  }
}
