package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;

public class SimulacionPlanTest {

  @Test
  public void elPorcentajeDeberiaCalcularseContraElTotalMasCaro() {
    SimulacionPlan simulacion = new SimulacionPlan(
      new Mascota(),
      12,
      List.of(dadoUnResultado(20000, 443000, true), dadoUnResultado(50000, 0, false)),
      List.of()
    );

    assertThat(simulacion.porcentaje(683000), is(100.0));
    assertThat(simulacion.porcentaje(240000), is(35.1));
    assertThat(simulacion.porcentaje(600000), is(87.8));
  }

  @Test
  public void sinResultadosElPorcentajeDeberiaSerCero() {
    SimulacionPlan simulacion = new SimulacionPlan(new Mascota(), 12, List.of(), List.of());

    assertThat(simulacion.porcentaje(1000), is(0.0));
  }

  private ResultadoPlan dadoUnResultado(double precioMensual, double noCubierto, boolean actual) {
    Plan plan = new Plan();
    plan.setPrecio(precioMensual);
    return new ResultadoPlan(plan, 12, noCubierto, actual);
  }
}
