package com.tallerwebi.dominio;

import java.util.List;
import java.util.stream.Collectors;

/** Los turnos de una mascota separados en próximos y pasados. */
public class HistorialTurnos {

  private final List<Turno> proximos;
  private final List<Turno> pasados;

  public HistorialTurnos(List<Turno> proximos, List<Turno> pasados) {
    this.proximos = proximos;
    this.pasados = pasados;
  }

  public List<Turno> getProximos() {
    return proximos;
  }

  public List<Turno> getPasados() {
    return pasados;
  }

  /** Historial clínico: las atenciones ya realizadas (turnos FINALIZADOS), de la más reciente a la más antigua. */
  public List<Turno> getAtenciones() {
    return pasados.stream().filter(Turno::estaFinalizado).collect(Collectors.toList());
  }
}
