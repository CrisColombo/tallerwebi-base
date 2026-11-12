package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.util.Set;

/** Una atención ya realizada, con el precio particular y qué planes la habrían cubierto. */
public class ItemAtencion {

  private final LocalDate fecha;
  private final String servicio;
  private final double precioParticular;
  private final Set<Long> planesQueCubren;

  public ItemAtencion(
    LocalDate fecha,
    String servicio,
    double precioParticular,
    Set<Long> planesQueCubren
  ) {
    this.fecha = fecha;
    this.servicio = servicio;
    this.precioParticular = precioParticular;
    this.planesQueCubren = planesQueCubren;
  }

  public boolean cubiertoPor(Long planId) {
    return planesQueCubren.contains(planId);
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public String getServicio() {
    return servicio;
  }

  public double getPrecioParticular() {
    return precioParticular;
  }
}
