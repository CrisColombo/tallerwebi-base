package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class ServicioSocioTest {

  @Test
  public void buscarPorUsuarioNuloNoDeberiaConsultarElRepositorio() {
    RepositorioSocio repositorioSocio = mock(RepositorioSocio.class);

    Socio socio = new ServicioSocioImpl(repositorioSocio).buscarPorUsuario(null);

    assertThat(socio, nullValue());
    verify(repositorioSocio, never()).buscarPorUsuario(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void buscarPorUsuarioDeberiaDevolverElSocioGuardado() {
    RepositorioSocio repositorioSocio = mock(RepositorioSocio.class);
    Socio esperado = new Socio();
    when(repositorioSocio.buscarPorUsuario(4L)).thenReturn(esperado);

    Socio socio = new ServicioSocioImpl(repositorioSocio).buscarPorUsuario(4L);

    assertThat(socio, equalTo(esperado));
  }
}
