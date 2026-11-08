package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ServicioLoginTest {

  private ServicioLogin servicioLogin;
  private RepositorioUsuario repositorioUsuarioMock;
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  public void init() {
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.servicioLogin = new ServicioLoginImpl(this.repositorioUsuarioMock, this.passwordEncoder);
  }

  @Test
  public void consultarUsuarioConLaContrasenaCorrectaDeberiaDevolverElUsuario() {
    // preparacion
    String email = "test@test.com";
    String password = "password";
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword(this.passwordEncoder.encode(password));
    when(this.repositorioUsuarioMock.buscar(email)).thenReturn(usuario);

    // ejecucion
    Usuario usuarioObtenido = this.servicioLogin.consultarUsuario(email, password);

    // validacion
    assertThat(usuarioObtenido, equalTo(usuario));
  }

  @Test
  public void consultarUsuarioConLaContrasenaIncorrectaDeberiaDevolverNull() {
    // preparacion
    String email = "test@test.com";
    String password = "password";
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword(this.passwordEncoder.encode(password));
    when(this.repositorioUsuarioMock.buscar(email)).thenReturn(usuario);

    // ejecucion
    Usuario usuarioObtenido = this.servicioLogin.consultarUsuario(email, "contrasenia");

    // validacion
    assertThat(usuarioObtenido, is(nullValue()));
  }

  @Test
  public void consultarUsuarioConEmailInexistenteDeberiaDevolverNull() {
    String email = "test@test.com";
    String password = "password";
    when(this.repositorioUsuarioMock.buscar(email)).thenReturn(null);

    Usuario usuarioSinEmail = this.servicioLogin.consultarUsuario(email, password);
    assertThat(usuarioSinEmail, is(nullValue()));
  }
}
