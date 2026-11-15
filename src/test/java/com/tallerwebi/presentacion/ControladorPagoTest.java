package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.PagoRechazado;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPagoTest {

  private ControladorPago controlador;
  private ServicioPago servicioPago;
  private HttpSession sesion;

  @BeforeEach
  public void init() {
    ServicioSocio servicioSocio = mock(ServicioSocio.class);
    servicioPago = mock(ServicioPago.class);
    controlador = new ControladorPago(servicioSocio, servicioPago);
    sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    Socio socio = new Socio();
    socio.setUsuario(new Usuario());
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(socio);
  }

  @Test
  public void pagarPlanDeberiaRedirigirAMercadoPago() throws Exception {
    when(servicioPago.iniciarPagoPlan(any(), eq(1L), eq(2L)))
      .thenReturn("https://sandbox.mercadopago.com/checkout");

    ModelAndView mav = controlador.pagarPlan(1L, 2L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:https://sandbox.mercadopago.com/checkout"));
  }

  @Test
  public void pagoExitoDeberiaConfirmarYMostrarLaVista() throws Exception {
    ModelAndView mav = controlador.pagoExito("999", null, "mascota-1-plan-2", sesion);

    verify(servicioPago).confirmarPago(any(), eq("999"), eq("mascota-1-plan-2"));
    assertThat(mav.getViewName(), equalTo("pago-exito"));
    assertThat(mav.getModel().get("mascotaId"), equalTo(1L));
  }

  @Test
  public void pagoExitoDeberiaUsarCollectionIdSiNoHayPaymentId() throws Exception {
    controlador.pagoExito("null", "888", "mascota-1-plan-2", sesion);

    verify(servicioPago).confirmarPago(any(), eq("888"), eq("mascota-1-plan-2"));
  }

  @Test
  public void pagoExitoSinIdDeberiaMostrarError() throws Exception {
    doThrow(new PagoRechazado("El pago no fue aprobado"))
      .when(servicioPago)
      .confirmarPago(any(), isNull(), eq("mascota-1-plan-2"));

    ModelAndView mav = controlador.pagoExito(null, null, "mascota-1-plan-2", sesion);

    assertThat(mav.getViewName(), equalTo("pago-error"));
    assertThat(mav.getModel().get("mensaje"), equalTo("El pago no fue aprobado"));
  }

  @Test
  public void pagoErrorDeberiaMostrarLaVistaSinConfirmar() throws Exception {
    ModelAndView mav = controlador.pagoError("mascota-1-plan-2");

    verify(servicioPago, never()).confirmarPago(any(), any(), any());
    assertThat(mav.getViewName(), equalTo("pago-error"));
    assertThat(mav.getModel().get("mascotaId"), equalTo(1L));
    assertThat(mav.getModel().get("mensaje"), equalTo("No se pudo completar el pago."));
  }

  @Test
  public void pagarPlanSinSocioDeberiaVolverAlLogin() throws Exception {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(null);

    ModelAndView mav = controlador.pagarPlan(1L, 2L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioPago, never()).iniciarPagoPlan(any(), any(), any());
  }

  @Test
  public void pagarPlanRechazadoDeberiaVolverAlPerfilConElError() throws Exception {
    when(servicioPago.iniciarPagoPlan(any(), eq(1L), eq(2L)))
      .thenThrow(new PagoRechazado("Plan inválido"));

    ModelAndView mav = controlador.pagarPlan(1L, 2L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/mascotas/1?errorPago=Plan inválido"));
  }

  @Test
  public void pagoExitoSinSocioDeberiaVolverAlLogin() throws Exception {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(null);

    ModelAndView mav = controlador.pagoExito("999", null, "mascota-1-plan-2", sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioPago, never()).confirmarPago(any(), any(), any());
  }

  @Test
  public void pagoExitoConIdEnBlancoDeberiaTratarloComoAusente() throws Exception {
    doThrow(new PagoRechazado("El pago no fue aprobado"))
      .when(servicioPago)
      .confirmarPago(any(), isNull(), eq("mascota-1-plan-2"));

    controlador.pagoExito("   ", "null", "mascota-1-plan-2", sesion);

    verify(servicioPago).confirmarPago(any(), isNull(), eq("mascota-1-plan-2"));
  }

  @Test
  public void pagoPendienteSinReferenciaNoDeberiaMostrarUnaMascota() {
    ModelAndView mav = controlador.pagoPendiente(null);

    assertThat(mav.getViewName(), equalTo("pago-pendiente"));
    assertThat(mav.getModel().get("mascotaId"), equalTo(null));
    assertThat(mav.getModel().get("mensaje"), equalTo(null));
  }

  @Test
  public void pagoPendienteNoDeberiaConfirmarElPago() throws Exception {
    ModelAndView mav = controlador.pagoPendiente("mascota-1-plan-2");

    verify(servicioPago, never()).confirmarPago(any(), any(), any());
    assertThat(mav.getViewName(), equalTo("pago-pendiente"));
    assertThat(mav.getModel().get("mascotaId"), equalTo(1L));
  }
}
