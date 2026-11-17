package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Pendientes;
import com.tallerwebi.dominio.ServicioPendientes;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorHomeTest {

  private ServicioSocio servicioSocio;
  private ServicioPendientes servicioPendientes;
  private ControladorHome controlador;
  private HttpSession sesion;
  private Socio socio;
  private Pendientes pendientes;

  @BeforeEach
  public void init() {
    servicioSocio = mock(ServicioSocio.class);
    servicioPendientes = mock(ServicioPendientes.class);
    controlador = new ControladorHome(servicioSocio, servicioPendientes);
    sesion = mock(HttpSession.class);
    socio = new Socio();
    pendientes = new Pendientes(List.of(), List.of(), List.of(), List.of(), List.of());
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(socio);
    when(servicioPendientes.pendientesDe(socio)).thenReturn(pendientes);
  }

  @Test
  public void elInicioDeberiaMostrarLosPendientesDelSocio() {
    ModelAndView mav = controlador.irAHome(sesion);

    assertThat(mav.getViewName(), equalTo("home"));
    assertThat(mav.getModel().get("socio"), is(socio));
    assertThat(mav.getModel().get("pendientes"), is(pendientes));
  }

  @Test
  public void unaVeterinariaDeberiaIrASuAgenda() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn(Usuario.ROL_VETERINARIA);

    ModelAndView mav = controlador.irAHome(sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/inicio"));
    verifyNoInteractions(servicioPendientes);
  }

  @Test
  public void unUsuarioSinSocioDeberiaVerElInicioSinPendientes() {
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(null);

    ModelAndView mav = controlador.irAHome(sesion);

    assertThat(mav.getViewName(), equalTo("home"));
    assertThat(mav.getModel().get("pendientes"), is(nullValue()));
  }

  @Test
  public void misMascotasDeberiaMostrarLasMascotasDelSocio() {
    ModelAndView mav = controlador.misMascotas(sesion);

    assertThat(mav.getViewName(), equalTo("mis-mascotas"));
    assertThat(mav.getModel().get("socio"), is(socio));
    assertThat(mav.getModel().get("pendientes"), is(pendientes));
  }

  @Test
  public void misMascotasSinSocioDeberiaVolverAlInicio() {
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(null);

    assertThat(controlador.misMascotas(sesion).getViewName(), equalTo("redirect:/home"));
  }
}
