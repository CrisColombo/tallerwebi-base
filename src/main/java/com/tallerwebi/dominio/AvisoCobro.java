package com.tallerwebi.dominio;

/** Mensaje que se le muestra al socio al sacar turno sobre cuánto va a pagar. */
public class AvisoCobro {

  private final String mensaje;
  private final boolean advertencia;

  public AvisoCobro(String mensaje, boolean advertencia) {
    this.mensaje = mensaje;
    this.advertencia = advertencia;
  }

  public String getMensaje() {
    return mensaje;
  }

  public boolean isAdvertencia() {
    return advertencia;
  }
}
