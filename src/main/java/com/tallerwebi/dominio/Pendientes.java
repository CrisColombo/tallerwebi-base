package com.tallerwebi.dominio;

import java.util.List;
import java.util.Objects;

/** Lo que el socio tiene que ver o resolver al entrar: turnos, devoluciones, recetas y planes. */
public class Pendientes {

  private final List<Turno> proximosTurnos;
  private final List<Turno> consultasRecientes;
  private final List<Turno> turnosSinResena;
  private final List<Receta> recetasSinComprar;
  private final List<Mascota> planesPorVencer;

  public Pendientes(
    List<Turno> proximosTurnos,
    List<Turno> consultasRecientes,
    List<Turno> turnosSinResena,
    List<Receta> recetasSinComprar,
    List<Mascota> planesPorVencer
  ) {
    this.proximosTurnos = proximosTurnos;
    this.consultasRecientes = consultasRecientes;
    this.turnosSinResena = turnosSinResena;
    this.recetasSinComprar = recetasSinComprar;
    this.planesPorVencer = planesPorVencer;
  }

  public Turno getProximoTurno() {
    return proximosTurnos.isEmpty() ? null : proximosTurnos.get(0);
  }

  public Turno proximoTurnoDe(Mascota mascota) {
    return proximosTurnos
      .stream()
      .filter(t -> Objects.equals(t.getMascota().getId(), mascota.getId()))
      .findFirst()
      .orElse(null);
  }

  /** Cantidad de cosas para resolver; el próximo turno no cuenta porque no requiere acción. */
  public int getCantidad() {
    return (
      consultasRecientes.size() +
      turnosSinResena.size() +
      recetasSinComprar.size() +
      planesPorVencer.size()
    );
  }

  public boolean isVacio() {
    return getCantidad() == 0;
  }

  public List<Turno> getProximosTurnos() {
    return proximosTurnos;
  }

  public List<Turno> getConsultasRecientes() {
    return consultasRecientes;
  }

  public List<Turno> getTurnosSinResena() {
    return turnosSinResena;
  }

  public List<Receta> getRecetasSinComprar() {
    return recetasSinComprar;
  }

  public List<Mascota> getPlanesPorVencer() {
    return planesPorVencer;
  }
}
