package com.tallerwebi.presentacion.controladores;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPlanTest {

  @Test
  public void mostrarPlanesDeberiaAbrirLaVistaDePlanes() {
    ModelAndView vista = new ControladorPlan().mostrarPlanes();

    assertThat(vista.getViewName(), equalTo("planes"));
  }
}
