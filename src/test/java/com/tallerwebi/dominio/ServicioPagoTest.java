package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.PagoRechazado;
import com.tallerwebi.infraestructura.ClienteMercadoPago;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPagoTest {

  private ServicioPago servicioPago;
  private ClienteMercadoPago clienteMercadoPago;
  private ServicioMascota servicioMascota;
  private RepositorioPlan repositorioPlan;
  private Socio socio;
  private Mascota mascota;
  private Plan plan;

  @BeforeEach
  public void init() {
    clienteMercadoPago = mock(ClienteMercadoPago.class);
    servicioMascota = mock(ServicioMascota.class);
    repositorioPlan = mock(RepositorioPlan.class);
    servicioPago = new ServicioPagoImpl(clienteMercadoPago, servicioMascota, repositorioPlan);

    socio = new Socio();
    mascota = new Mascota();
    mascota.setId(1L);
    mascota.setNombre("Firulais");
    plan = new Plan();
    plan.setId(2L);
    plan.setNombre("Plan Premium");
    plan.setPrecio(50000.0);

    when(servicioMascota.buscarPorIdParaSocio(socio, 1L)).thenReturn(mascota);
    when(repositorioPlan.buscarPorId(2L)).thenReturn(plan);
  }

  @Test
  public void iniciarPagoDeberiaDevolverUrlDeMercadoPago() throws Exception {
    when(clienteMercadoPago.crearPreferencia(anyString(), eq(50000.0), anyString()))
      .thenReturn("https://sandbox.mercadopago.com/...");

    String url = servicioPago.iniciarPagoPlan(socio, 1L, 2L);

    assertThat(url, equalTo("https://sandbox.mercadopago.com/..."));
    verify(clienteMercadoPago)
      .crearPreferencia(eq("Plan Premium - Firulais"), eq(50000.0), eq("mascota-1-plan-2"));
  }

  @Test
  public void confirmarPagoAprobadoDeberiaCambiarPlan() throws Exception {
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);
    when(servicioMascota.buscarPorId(1L)).thenReturn(mascota);

    servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2");

    verify(servicioMascota).cambiarPlan(socio, 1L, 2L);
  }

  @Test
  public void confirmarPagoRechazadoNoDeberiaCambiarPlan() {
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(false);

    assertThrows(
      PagoRechazado.class,
      () -> servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2")
    );

    verify(servicioMascota, never()).cambiarPlan(any(), anyLong(), anyLong());
  }

  @Test
  public void pagoRepetidoNoDeberiaSumarOtroMes() throws Exception {
    LocalDate vence = LocalDate.now().plusDays(10);
    mascota.setVenceSuscripcion(vence);
    mascota.setUltimoPagoMercadoPago("999");
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);
    when(servicioMascota.buscarPorId(1L)).thenReturn(mascota);

    servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2");

    assertThat(mascota.getVenceSuscripcion(), equalTo(vence));
  }

  @Test
  public void confirmarPagoSinIdNoDeberiaCambiarPlan() {
    assertThrows(
      PagoRechazado.class,
      () -> servicioPago.confirmarPago(socio, null, "mascota-1-plan-2")
    );

    verify(servicioMascota, never()).cambiarPlan(any(), anyLong(), anyLong());
  }

  @Test
  public void iniciarPagoDeUnaMascotaAjenaDeberiaRechazarlo() {
    when(servicioMascota.buscarPorIdParaSocio(socio, 1L)).thenReturn(null);

    PagoRechazado error = assertThrows(
      PagoRechazado.class,
      () -> servicioPago.iniciarPagoPlan(socio, 1L, 2L)
    );

    assertThat(error.getMessage(), equalTo("No encontramos esa mascota"));
  }

  @Test
  public void iniciarPagoConPlanSinPrecioDeberiaRechazarlo() {
    plan.setPrecio(null);

    PagoRechazado error = assertThrows(
      PagoRechazado.class,
      () -> servicioPago.iniciarPagoPlan(socio, 1L, 2L)
    );

    assertThat(error.getMessage(), equalTo("Plan inválido"));
  }

  @Test
  public void confirmarPagoConReferenciaInvalidaDeberiaRechazarlo() {
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);

    PagoRechazado error = assertThrows(
      PagoRechazado.class,
      () -> servicioPago.confirmarPago(socio, "999", "cualquiera")
    );

    assertThat(error.getMessage(), equalTo("Referencia de pago inválida"));
    verify(servicioMascota, never()).cambiarPlan(any(), anyLong(), anyLong());
  }

  @Test
  public void confirmarPagoSiLaMascotaDesapareceDeberiaRechazarlo() {
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);
    when(servicioMascota.buscarPorId(1L)).thenReturn(null);

    PagoRechazado error = assertThrows(
      PagoRechazado.class,
      () -> servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2")
    );

    assertThat(error.getMessage(), equalTo("No encontramos esa mascota"));
  }

  @Test
  public void iniciarElPlanActualAlDiaNoDeberiaLlamarAMercadoPago() {
    plan.setNivel(2);
    mascota.setPlan(plan);
    mascota.setVenceSuscripcion(LocalDate.now().plusDays(10));
    mascota.setUltimoPagoMercadoPago("111");

    PagoRechazado error = assertThrows(
      PagoRechazado.class,
      () -> servicioPago.iniciarPagoPlan(socio, 1L, 2L)
    );

    assertThat(error.getMessage(), equalTo("Ya tenés este plan activo"));
    verify(clienteMercadoPago, never()).crearPreferencia(anyString(), eq(50000.0), anyString());
  }

  @Test
  public void unUpgradeDeberiaCobrarSoloElProporcional() throws Exception {
    Plan basico = new Plan();
    basico.setId(1L);
    basico.setNombre("Básico");
    basico.setNivel(1);
    basico.setPrecio(20000.0);
    plan.setNivel(2);
    mascota.setPlan(basico);
    mascota.setVenceSuscripcion(LocalDate.now().plusMonths(1));
    mascota.setUltimoPagoMercadoPago("111");
    when(
      clienteMercadoPago.crearPreferencia(
        anyString(),
        org.mockito.ArgumentMatchers.anyDouble(),
        anyString()
      )
    )
      .thenReturn("https://sandbox.mercadopago.com/upgrade");
    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(
      mascota,
      plan,
      LocalDate.now()
    );

    String url = servicioPago.iniciarPagoPlan(socio, 1L, 2L);

    assertThat(url, equalTo("https://sandbox.mercadopago.com/upgrade"));
    assertThat(cotizacion.getImporte(), lessThan(plan.getPrecio()));
    verify(clienteMercadoPago)
      .crearPreferencia(
        eq("Plan Premium - Firulais"),
        eq(cotizacion.getImporte()),
        eq("mascota-1-plan-2-upgrade")
      );
  }

  @Test
  public void confirmarUnUpgradeNoDeberiaMoverElVencimiento() throws Exception {
    LocalDate vence = LocalDate.now().plusDays(15);
    mascota.setVenceSuscripcion(vence);
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);
    when(servicioMascota.buscarPorId(1L)).thenReturn(mascota);

    servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2-upgrade");

    verify(servicioMascota).cambiarPlan(socio, 1L, 2L);
    assertThat(mascota.getVenceSuscripcion(), equalTo(vence));
  }

  @Test
  public void confirmarUnaRenovacionDeberiaSumarUnMes() throws Exception {
    mascota.setVenceSuscripcion(LocalDate.now().minusDays(1));
    when(clienteMercadoPago.estaAprobado("999")).thenReturn(true);
    when(servicioMascota.buscarPorId(1L)).thenReturn(mascota);

    servicioPago.confirmarPago(socio, "999", "mascota-1-plan-2");

    assertThat(mascota.getVenceSuscripcion(), equalTo(LocalDate.now().plusMonths(1)));
  }
}
