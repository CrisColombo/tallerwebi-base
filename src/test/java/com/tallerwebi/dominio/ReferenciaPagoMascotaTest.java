package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class ReferenciaPagoMascotaTest {

  @Test
  public void deberiaArmarYLeerLaReferenciaDeMascotaYPlan() {
    String referencia = ReferenciaPagoMascota.armar(5L, 2L);

    assertThat(referencia, equalTo("mascota-5-plan-2"));
    assertThat(ReferenciaPagoMascota.mascotaIdDe(referencia), equalTo(5L));
    assertThat(ReferenciaPagoMascota.planIdDe(referencia), equalTo(2L));
    assertThat(ReferenciaPagoMascota.esUpgrade(referencia), equalTo(false));
  }

  @Test
  public void unaReferenciaDeUpgradeDeberiaLeerLosIdsYMarcarElCambio() {
    String referencia = ReferenciaPagoMascota.armarUpgrade(5L, 2L);

    assertThat(referencia, equalTo("mascota-5-plan-2-upgrade"));
    assertThat(ReferenciaPagoMascota.mascotaIdDe(referencia), equalTo(5L));
    assertThat(ReferenciaPagoMascota.planIdDe(referencia), equalTo(2L));
    assertThat(ReferenciaPagoMascota.esUpgrade(referencia), equalTo(true));
  }

  @Test
  public void unaReferenciaVaciaOMalFormadaNoDeberiaDevolverIds() {
    assertThat(ReferenciaPagoMascota.mascotaIdDe(null), nullValue());
    assertThat(ReferenciaPagoMascota.planIdDe("pago-1"), nullValue());
    assertThat(ReferenciaPagoMascota.mascotaIdDe("mascota-5"), nullValue());
    assertThat(ReferenciaPagoMascota.planIdDe("mascota-5-plan"), nullValue());
  }
}
