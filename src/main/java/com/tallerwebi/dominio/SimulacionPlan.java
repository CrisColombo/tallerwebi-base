package com.tallerwebi.dominio;

import java.util.Comparator;
import java.util.List;

/** Resultado completo del simulador para una mascota. */
public class SimulacionPlan {

  private final Mascota mascota;
  private final int meses;
  private final List<ResultadoPlan> resultados;
  private final List<ItemAtencion> atenciones;

  public SimulacionPlan(
    Mascota mascota,
    int meses,
    List<ResultadoPlan> resultados,
    List<ItemAtencion> atenciones
  ) {
    this.mascota = mascota;
    this.meses = meses;
    this.resultados = resultados;
    this.atenciones = atenciones;
  }

  /** El más barato del período; si empata con el plan actual, gana el actual. */
  public ResultadoPlan getMasConveniente() {
    Comparator<ResultadoPlan> porPrecio = Comparator.comparingDouble(ResultadoPlan::getTotal);
    return resultados.stream().min(porPrecio.thenComparing(r -> !r.isActual())).orElse(null);
  }

  public ResultadoPlan getPlanActual() {
    return resultados.stream().filter(ResultadoPlan::isActual).findFirst().orElse(null);
  }

  /** Cuánto se ahorraba con el plan más conveniente respecto del actual (0 si ya era el mejor). */
  public double getAhorro() {
    ResultadoPlan actual = getPlanActual();
    ResultadoPlan mejor = getMasConveniente();
    if (actual == null || mejor == null) {
      return 0;
    }
    return actual.getTotal() - mejor.getTotal();
  }

  public boolean isPlanActualEsElMejor() {
    return getAhorro() <= 0;
  }

  public boolean isSinAtenciones() {
    return atenciones.isEmpty();
  }

  public Mascota getMascota() {
    return mascota;
  }

  public int getMeses() {
    return meses;
  }

  public List<ResultadoPlan> getResultados() {
    return resultados;
  }

  public List<ItemAtencion> getAtenciones() {
    return atenciones;
  }
}
