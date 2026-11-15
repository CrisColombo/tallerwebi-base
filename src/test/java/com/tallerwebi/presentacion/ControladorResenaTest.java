package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorResenaTest {

  private ControladorResena controlador;
  private ServicioResena servicioResena;
  private HttpSession sesion;

  @BeforeEach
  public void init() {
    ServicioSocio servicioSocio = mock(ServicioSocio.class);
    servicioResena = mock(ServicioResena.class);
    controlador = new ControladorResena(servicioSocio, servicioResena);
    sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(new Socio());
  }

  @Test
  public void crearResenaDeberiaRedirigirConOk() throws ResenaInvalida {
    ModelAndView mav = controlador.crearResena(10L, 5, "Muy buena atención", sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/turnos?resenaOk=1"));
  }

  @Test
  public void resenaInvalidaDeberiaRedirigirConError() throws ResenaInvalida {
    doThrow(new ResenaInvalida("Escribí un comentario"))
      .when(servicioResena)
      .crearResena(any(), eq(10L), eq(4), eq(""));

    ModelAndView mav = controlador.crearResena(10L, 4, "", sesion);

    assertThat(mav.getViewName(), containsString("redirect:/turnos?errorResena="));
    assertThat(mav.getViewName(), containsString("Escrib"));
  }
}
