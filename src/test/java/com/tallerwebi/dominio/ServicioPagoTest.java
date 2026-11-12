package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
}
