package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CobroMensualTest {

  private Plan basico;
  private Servicio consulta;
  private Servicio cirugia;

  @BeforeEach
  public void init() {
    basico = new Plan();
    basico.setNivel(1);
    basico.setTopeMensual(2);
    basico.setCopago(2000.0);
    consulta = dadoUnServicio(1);
    cirugia = dadoUnServicio(2);
  }

  @Test
  public void deberiaCobrarTodoSiLaMascotaNoTienePlan() {
    CobroMensual cobro = new CobroMensual(null, 0);

    assertThat(cobro.cobrar(consulta, 8000.0), equalTo(8000.0));
  }

  @Test
  public void deberiaCobrarSoloElCopagoSiElPlanCubreElServicio() {
    CobroMensual cobro = new CobroMensual(basico, 0);

    assertThat(cobro.cobrar(consulta, 8000.0), equalTo(2000.0));
  }

  @Test
  public void deberiaCobrarPrecioCompletoCuandoSeSuperaElTope() {
    CobroMensual cobro = new CobroMensual(basico, 0);

    cobro.cobrar(consulta, 8000.0);
    cobro.cobrar(consulta, 8000.0);

    assertThat(cobro.cobrar(consulta, 8000.0), equalTo(8000.0));
  }

  @Test
  public void deberiaContarLasAtencionesPreviasDelMes() {
    CobroMensual cobro = new CobroMensual(basico, 2);

    assertThat(cobro.cobrar(consulta, 8000.0), equalTo(8000.0));
  }

  @Test
  public void noDeberiaGastarElTopeConServiciosQueElPlanNoCubre() {
    CobroMensual cobro = new CobroMensual(basico, 0);

    assertThat(cobro.cobrar(cirugia, 400000.0), equalTo(400000.0));
    assertThat(cobro.getCubiertas(), equalTo(0));
  }

  @Test
  public void noDeberiaCobrarMasQueElPrecioParticularAunqueElCopagoSeaMayor() {
    CobroMensual cobro = new CobroMensual(basico, 0);

    assertThat(cobro.cobrar(consulta, 1500.0), equalTo(1500.0));
  }

  private Servicio dadoUnServicio(Integer nivelRequerido) {
    Servicio servicio = new Servicio();
    servicio.setNivelRequerido(nivelRequerido);
    return servicio;
  }
}
