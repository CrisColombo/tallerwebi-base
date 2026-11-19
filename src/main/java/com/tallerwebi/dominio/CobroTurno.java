package com.tallerwebi.dominio;

import java.util.List;

/** Lo que cuesta un turno y lo que le toca pagar al socio según su plan. */
public class CobroTurno {

  private final List<LineaCobro> lineas;

  public CobroTurno(List<LineaCobro> lineas) {
    this.lineas = List.copyOf(lineas);
  }

  public List<LineaCobro> getLineas() {
    return lineas;
  }

  public double getTotalParticular() {
    double total = 0;
    for (LineaCobro linea : lineas) {
      total += linea.getPrecioParticular();
    }
    return total;
  }

  public double getTotalSocio() {
    double total = 0;
    for (LineaCobro linea : lineas) {
      total += linea.getPagaElSocio();
    }
    return total;
  }
}
