package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

public class TurnoAdicionalesTest {

  @Test
  public void deberiaSumarLosAdicionalesAlPrecioDelTurno() {
    Turno turno = new Turno();
    turno.setPrecioParticular(8000.0);

    turno.agregarAdicional(new Servicio(), 15000.0);
    turno.agregarAdicional(new Servicio(), 12000.0);

    assertThat(turno.getAdicionales(), hasSize(2));
    assertThat(turno.getTotalParticular(), equalTo(35000.0));
  }

  @Test
  public void deberiaTenerTotalCeroSiNoTienePrecioNiAdicionales() {
    assertThat(new Turno().getTotalParticular(), equalTo(0.0));
  }
}
