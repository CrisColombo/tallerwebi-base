package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.util.Map;

/** Una atención ya realizada, con su precio particular y lo que se paga con cada plan. */
public class ItemAtencion {

  private final LocalDate fecha;
  private final String servicio;
  private final double precioParticular;
  private final Map<Long, Double> pagoPorPlan;

  public ItemAtencion(
    LocalDate fecha,
    String servicio,
    double precioParticular,
    Map<Long, Double> pagoPorPlan
  ) {
    this.fecha = fecha;
    this.servicio = servicio;
    this.precioParticular = precioParticular;
    this.pagoPorPlan = pagoPorPlan;
  }

  /** Lo que el dueño paga de su bolsillo por esta atención con ese plan. */
  public double pagoCon(Long planId) {
    return pagoPorPlan.getOrDefault(planId, precioParticular);
  }

  /** Cubierta del todo: con ese plan no se paga nada aparte. */
  public boolean cubiertoPor(Long planId) {
    return pagoCon(planId) == 0;
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
