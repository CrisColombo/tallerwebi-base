package com.tallerwebi.dominio;

/** Lo que paga el socio por cada atención de un mes, según su plan (tope y copago). */
public class CobroMensual {

  private final Plan plan;
  private int cubiertas;

  public CobroMensual(Plan plan, int cubiertasPrevias) {
    this.plan = plan;
    this.cubiertas = cubiertasPrevias;
  }

  /** Va sumando atenciones: llamarlo en orden cronológico, una vez por atención. */
  public double cobrar(Servicio servicio, double precioParticular) {
    if (plan == null || !plan.cubre(servicio)) {
      return precioParticular;
    }
    cubiertas++;
    if (!plan.dentroDelTope(cubiertas)) {
      return precioParticular;
    }
    return Math.min(precioParticular, plan.copagoPorAtencion());
  }

  public int getCubiertas() {
    return cubiertas;
  }
}
