package com.tallerwebi.dominio;

/** Un servicio del turno con su precio particular y lo que paga el socio. */
public class LineaCobro {

  private final String servicio;
  private final double precioParticular;
  private final double pagaElSocio;

  public LineaCobro(String servicio, double precioParticular, double pagaElSocio) {
    this.servicio = servicio;
    this.precioParticular = precioParticular;
    this.pagaElSocio = pagaElSocio;
  }

  public String getServicio() {
    return servicio;
  }

  public double getPrecioParticular() {
    return precioParticular;
  }

  public double getPagaElSocio() {
    return pagaElSocio;
  }

  public boolean isCubierto() {
    return pagaElSocio == 0;
  }

  public boolean isCopago() {
    return pagaElSocio > 0 && pagaElSocio < precioParticular;
  }
}
