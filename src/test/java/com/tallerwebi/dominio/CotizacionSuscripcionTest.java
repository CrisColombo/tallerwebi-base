package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class CotizacionSuscripcionTest {

  private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
  private static final LocalDate VENCE = LocalDate.of(2026, 12, 1);

  private final Plan basico = plan(1L, "Básico", 1, 20000);
  private final Plan premium = plan(2L, "Premium", 2, 40000);

  @Test
  public void elMismoPlanAlDiaNoSePuedeVolverAPagar() {
    Mascota mascota = mascota(basico, VENCE, "pago-1");

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, basico, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.BLOQUEADO));
    assertThat(cotizacion.getDetalle(), equalTo("Ya tenés este plan activo"));
    assertThat(cotizacion.permitePagar(), is(false));
  }

  @Test
  public void unUpgradeAMitadDeMesCobraQuinceDeTreintaDeLaDiferencia() {
    Mascota mascota = mascota(basico, VENCE, "pago-1");

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, premium, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.UPGRADE));
    assertThat(cotizacion.getImporte(), equalTo(10000.0));
    assertThat(cotizacion.getTextoBoton(), equalTo("Pagar diferencia"));
    assertThat(cotizacion.getDetalle(), containsString("$10.000"));
    assertThat(cotizacion.getDetalle(), containsString("15 días"));
    assertThat(cotizacion.getDetalle(), containsString("01/12/2026"));
    assertThat(cotizacion.esUpgrade(), is(true));
  }

  @Test
  public void unaSuscripcionVencidaCobraElMesEnteroParaRenovar() {
    Mascota mascota = mascota(basico, LocalDate.of(2026, 10, 1), "pago-1");

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, premium, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.RENOVACION));
    assertThat(cotizacion.getImporte(), equalTo(40000.0));
    assertThat(cotizacion.getTextoBoton(), equalTo("Renovar suscripción"));
    assertThat(cotizacion.getDetalle(), equalTo("Renovás un mes por $40.000"));
  }

  @Test
  public void bajarDePlanConLaSuscripcionAlDiaNoTieneCargo() {
    Mascota mascota = mascota(premium, VENCE, "pago-1");

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, basico, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.BAJA));
    assertThat(cotizacion.getImporte(), equalTo(0.0));
    assertThat(cotizacion.esBaja(), is(true));
    assertThat(cotizacion.permitePagar(), is(false));
    assertThat(
      cotizacion.getDetalle(),
      equalTo("Sin cargo. El plan baja y el vencimiento no cambia")
    );
  }

  @Test
  public void sinPagoPrevioEsUnAltaPorElPrecioMensual() {
    Mascota mascota = mascota(null, null, null);

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, basico, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.ALTA));
    assertThat(cotizacion.getImporte(), equalTo(20000.0));
    assertThat(cotizacion.getTextoBoton(), equalTo("Pagar con Mercado Pago"));
  }

  @Test
  public void unUpgradeElDiaDelVencimientoNoTieneDiferenciaParaCobrar() {
    Mascota mascota = mascota(basico, HOY, "pago-1");

    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(mascota, premium, HOY);

    assertThat(cotizacion.getTipo(), is(CotizacionSuscripcion.Tipo.BLOQUEADO));
    assertThat(
      cotizacion.getDetalle(),
      equalTo("No queda tiempo del mes para cobrar la diferencia")
    );
  }

  private Mascota mascota(Plan plan, LocalDate vence, String pago) {
    Mascota mascota = new Mascota();
    mascota.setPlan(plan);
    mascota.setVenceSuscripcion(vence);
    mascota.setUltimoPagoMercadoPago(pago);
    return mascota;
  }

  private Plan plan(Long id, String nombre, int nivel, double precio) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setNombre(nombre);
    plan.setNivel(nivel);
    plan.setPrecio(precio);
    return plan;
  }
}
