package com.tallerwebi.dominio;

import java.util.List;

public class ResumenResenas {

  private final double promedio;
  private final long cantidad;
  private final List<Resena> ultimas;

  public ResumenResenas(double promedio, long cantidad, List<Resena> ultimas) {
    this.promedio = promedio;
    this.cantidad = cantidad;
    this.ultimas = ultimas;
  }

  public double getPromedio() {
    return promedio;
  }

  public long getCantidad() {
    return cantidad;
  }

  public List<Resena> getUltimas() {
    return ultimas;
  }
}
