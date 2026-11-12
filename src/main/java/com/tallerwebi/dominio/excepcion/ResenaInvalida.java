package com.tallerwebi.dominio.excepcion;

public class ResenaInvalida extends Exception {

  private static final long serialVersionUID = 1L;

  public ResenaInvalida(String mensaje) {
    super(mensaje);
  }

  public ResenaInvalida(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
