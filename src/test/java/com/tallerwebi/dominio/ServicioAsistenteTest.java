package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.infraestructura.ClienteGemini;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioAsistenteTest {

  private ServicioAsistente servicio;
  private ClienteGemini clienteGemini;
  private HttpSession sesion;
  private Socio socio;

  @BeforeEach
  public void init() {
    clienteGemini = mock(ClienteGemini.class);
    sesion = mock(HttpSession.class);
    servicio = new ServicioAsistenteImpl(clienteGemini);

    socio = new Socio();
    Mascota luna = new Mascota();
    luna.setId(3L);
    luna.setNombre("Luna");
    luna.setEspecie(Especie.PERRO);
    luna.setRaza("Caniche");
    socio.agregarMascota(luna);

    when(clienteGemini.completar(any(), anyString())).thenReturn("Llevala a una consulta.");
  }

  @Test
  public void mensajeVacioNoDeberiaConsultarAGemini() {
    assertThat(servicio.enviarMensaje(socio, sesion, "   "), equalTo(""));
    assertThat(servicio.enviarMensaje(socio, sesion, null), equalTo(""));

    verify(clienteGemini, never()).completar(any(), anyString());
    verify(sesion, never()).setAttribute(anyString(), any());
  }

  @Test
  public void deberiaEnviarElContextoDeMascotasYGuardarLaConversacion() {
    when(sesion.getAttribute(ServicioAsistenteImpl.CLAVE_HISTORIAL_CHAT)).thenReturn(null);
    when(clienteGemini.completar(any(), anyString()))
      .thenAnswer(invocacion -> {
        List<?> historialEnviado = invocacion.getArgument(0);
        String pregunta = invocacion.getArgument(1);
        assertThat(historialEnviado, hasSize(0));
        assertThat(pregunta, containsString("[CONTEXTO]"));
        assertThat(pregunta, containsString("nombre=Luna"));
        assertThat(pregunta, containsString("raza=Caniche"));
        assertThat(pregunta, containsString("Pregunta del usuario: ¿Puede comer pollo?"));
        return "Con moderación, sí.";
      });

    String respuesta = servicio.enviarMensaje(socio, sesion, "  ¿Puede comer pollo?  ");

    assertThat(respuesta, equalTo("Con moderación, sí."));
    List<MensajeConversacion> guardado = historialGuardado();
    assertThat(guardado, hasSize(2));
    assertThat(guardado.get(0).getRol(), equalTo("user"));
    assertThat(guardado.get(0).getTexto(), equalTo("¿Puede comer pollo?"));
    assertThat(guardado.get(1).getRol(), equalTo("model"));
    assertThat(guardado.get(1).getTexto(), equalTo("Con moderación, sí."));
  }

  @Test
  public void siLaSesionNoTieneUnaListaDeberiaEmpezarUnHistorialNuevo() {
    when(sesion.getAttribute(ServicioAsistenteImpl.CLAVE_HISTORIAL_CHAT)).thenReturn("basura");

    servicio.enviarMensaje(socio, sesion, "Hola");

    assertThat(historialGuardado(), hasSize(2));
  }

  @Test
  public void deberiaReutilizarElHistorialYBorrarLosMensajesMasViejos() {
    List<MensajeConversacion> previo = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      previo.add(new MensajeConversacion("user", "mensaje-" + i));
    }
    when(sesion.getAttribute(ServicioAsistenteImpl.CLAVE_HISTORIAL_CHAT)).thenReturn(previo);

    servicio.enviarMensaje(socio, sesion, "nuevo");

    List<MensajeConversacion> guardado = historialGuardado();
    assertThat(guardado, hasSize(20));
    assertThat(guardado.get(0).getTexto(), equalTo("mensaje-2"));
    assertThat(guardado.get(18).getTexto(), equalTo("nuevo"));
    assertThat(guardado.get(19).getRol(), equalTo("model"));
  }

  @SuppressWarnings("unchecked")
  private List<MensajeConversacion> historialGuardado() {
    ArgumentCaptor<List<MensajeConversacion>> captor = ArgumentCaptor.forClass(List.class);
    verify(sesion)
      .setAttribute(
        org.mockito.ArgumentMatchers.eq(ServicioAsistenteImpl.CLAVE_HISTORIAL_CHAT),
        captor.capture()
      );
    return captor.getValue();
  }
}
