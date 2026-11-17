package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioAsistente;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

public class ControladorChatTest {

  private ControladorChat controlador;
  private ServicioSocio servicioSocio;
  private ServicioAsistente servicioAsistente;
  private HttpSession sesion;
  private Socio socio;

  @BeforeEach
  public void init() {
    servicioSocio = mock(ServicioSocio.class);
    servicioAsistente = mock(ServicioAsistente.class);
    sesion = mock(HttpSession.class);
    controlador = new ControladorChat(servicioSocio, servicioAsistente);
    socio = new Socio();
    socio.setId(4L);
  }

  @Test
  public void irAlChatDeberiaMostrarLaVistaConElSocioLogueado() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(4L);
    when(servicioSocio.buscarPorUsuario(4L)).thenReturn(socio);

    ModelAndView vista = controlador.irAlChat(sesion);

    assertThat(vista.getViewName(), equalTo("chat"));
    assertThat(vista.getModel().get("socio"), equalTo(socio));
  }

  @Test
  public void mensajeSinSocioDeberiaResponderNoAutorizado() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(null);

    RuntimeException error = assertThrows(
      RuntimeException.class,
      () -> controlador.mensaje("hola", sesion)
    );

    ResponseStatus estado = error.getClass().getAnnotation(ResponseStatus.class);
    assertThat(estado.value(), equalTo(HttpStatus.UNAUTHORIZED));
  }

  @Test
  public void mensajeDeUnSocioDeberiaDevolverLaRespuestaDelAsistente() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(4L);
    when(servicioSocio.buscarPorUsuario(4L)).thenReturn(socio);
    when(servicioAsistente.enviarMensaje(socio, sesion, "¿Está bien?")).thenReturn("Observala.");

    Map<String, String> cuerpo = controlador.mensaje("¿Está bien?", sesion);

    assertThat(cuerpo.get("respuesta"), equalTo("Observala."));
    verify(servicioAsistente).enviarMensaje(socio, sesion, "¿Está bien?");
  }
}
