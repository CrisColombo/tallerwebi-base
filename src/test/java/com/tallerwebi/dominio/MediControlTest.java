package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.infraestructura.ClienteGemini;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediControlTest {

  private MediControl mediControl;
  private ClienteGemini clienteGemini;

  @BeforeEach
  public void init() {
    clienteGemini = mock(ClienteGemini.class);
    mediControl = new MediControl(clienteGemini);
  }

  @Test
  public void respuestaSiDeberiaCensurar() {
    when(clienteGemini.completarConInstruccion(anyString(), anyString())).thenReturn("SI");

    assertThat(mediControl.debeCensurar("un insulto"), equalTo(true));
  }

  @Test
  public void respuestaNoDeberiaPublicarElComentario() {
    when(clienteGemini.completarConInstruccion(anyString(), anyString())).thenReturn("NO");

    assertThat(mediControl.debeCensurar("Muy buena atención"), equalTo(false));
  }

  @Test
  public void respuestaConAcentoYPuntuacionDeberiaContarComoSi() {
    assertThat(MediControl.respondeSi("Sí."), equalTo(true));
    assertThat(MediControl.respondeSi("  no  "), equalTo(false));
    assertThat(MediControl.respondeSi(null), equalTo(false));
    assertThat(MediControl.respondeSi("   "), equalTo(false));
  }

  @Test
  public void falloDeGeminiDeberiaDejarPasarElComentario() {
    when(clienteGemini.completarConInstruccion(anyString(), anyString()))
      .thenThrow(new IllegalStateException("sin red"));

    assertThat(mediControl.debeCensurar("Muy buena atención"), equalTo(false));
  }
}
