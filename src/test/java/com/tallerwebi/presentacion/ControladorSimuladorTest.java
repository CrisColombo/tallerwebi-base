package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.ServicioSimulador;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.SimulacionPlan;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorSimuladorTest {

  private static final Long MASCOTA_ID = 1L;

  private ServicioSocio servicioSocioMock;
  private ServicioSimulador servicioSimuladorMock;
  private ControladorSimulador controlador;
  private HttpSession sesion;
  private Socio socio;

  @BeforeEach
  public void init() {
    servicioSocioMock = mock(ServicioSocio.class);
    servicioSimuladorMock = mock(ServicioSimulador.class);
    controlador = new ControladorSimulador(servicioSocioMock, servicioSimuladorMock);
    sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    socio = new Socio();
    when(servicioSocioMock.buscarPorUsuario(1L)).thenReturn(socio);
  }

  @Test
  public void deberiaMostrarElSimuladorDeLaMascota() throws Exception {
    SimulacionPlan simulacion = new SimulacionPlan(new Mascota(), 12, List.of(), List.of());
    when(servicioSimuladorMock.simular(socio, MASCOTA_ID, 12)).thenReturn(simulacion);

    ModelAndView mav = controlador.verSimulador(MASCOTA_ID, 12, sesion);

    assertThat(mav.getViewName(), equalTo("simulador"));
    assertThat(mav.getModel().get("simulacion"), is(simulacion));
  }

  @Test
  public void sinSocioLogueadoDeberiaRedirigirAlHome() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(null);

    ModelAndView mav = controlador.verSimulador(MASCOTA_ID, 12, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioSimuladorMock);
  }

  @Test
  public void conUnaMascotaAjenaDeberiaRedirigirAlHome() throws Exception {
    when(servicioSimuladorMock.simular(socio, MASCOTA_ID, 12)).thenReturn(null);

    ModelAndView mav = controlador.verSimulador(MASCOTA_ID, 12, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void siElServicioFallaDeberiaRedirigirAlHome() throws Exception {
    when(servicioSimuladorMock.simular(socio, MASCOTA_ID, 12))
      .thenThrow(new TurnoInvalido("No encontramos esa mascota"));

    ModelAndView mav = controlador.verSimulador(MASCOTA_ID, 12, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void deberiaLimitarElPeriodoEntreUnoYSesentaMeses() throws Exception {
    when(servicioSimuladorMock.simular(socio, MASCOTA_ID, 60)).thenReturn(null);
    when(servicioSimuladorMock.simular(socio, MASCOTA_ID, 1)).thenReturn(null);

    controlador.verSimulador(MASCOTA_ID, 999, sesion);
    controlador.verSimulador(MASCOTA_ID, 0, sesion);

    verify(servicioSimuladorMock).simular(socio, MASCOTA_ID, 60);
    verify(servicioSimuladorMock).simular(socio, MASCOTA_ID, 1);
  }
}
