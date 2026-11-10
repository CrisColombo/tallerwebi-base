package com.tallerwebi.dominio;

public class DatosReceta {

  private final String medicamento;
  private final String indicaciones;

  public DatosReceta(String medicamento, String indicaciones) {
    this.medicamento = medicamento;
    this.indicaciones = indicaciones;
  }

  public String getMedicamento() {
    return medicamento;
  }

  public String getIndicaciones() {
    return indicaciones;
  }
}
