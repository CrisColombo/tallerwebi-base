package com.tallerwebi.dominio;

import java.util.ArrayList;
import java.util.List;

public class RecetasDeConsulta {

  private final Turno turno;
  private final List<Receta> recetas = new ArrayList<>();

  public RecetasDeConsulta(Turno turno) {
    this.turno = turno;
  }

  public void agregar(Receta receta) {
    recetas.add(receta);
  }

  public Turno getTurno() {
    return turno;
  }

  public List<Receta> getRecetas() {
    return recetas;
  }
}
