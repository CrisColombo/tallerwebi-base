package com.tallerwebi.dominio;

/** Lo que habría costado el período con un plan: cuotas más lo que ese plan no cubre. */
public class ResultadoPlan {

  private final Plan plan;
  private final int meses;
  private final double noCubierto;
  private final boolean actual;

  public ResultadoPlan(Plan plan, int meses, double noCubierto, boolean actual) {
    this.plan = plan;
    this.meses = meses;
    this.noCubierto = noCubierto;
    this.actual = actual;
  }

  public double getCuotaTotal() {
    return plan.getPrecio() * meses;
  }

  public double getTotal() {
    return getCuotaTotal() + noCubierto;
  }

  public Plan getPlan() {
    return plan;
  }

  public double getNoCubierto() {
    return noCubierto;
  }

  public boolean isActual() {
    return actual;
  }
}
