package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Usuario;
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
}
