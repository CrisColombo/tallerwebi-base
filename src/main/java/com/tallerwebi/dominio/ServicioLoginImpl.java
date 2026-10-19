package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("servicioLogin")
@Transactional
public class ServicioLoginImpl implements ServicioLogin {

  private RepositorioUsuario repositorioUsuario;
  private PasswordEncoder passwordEncoder;

  @Autowired
  public ServicioLoginImpl(RepositorioUsuario repositorioUsuario, PasswordEncoder passwordEncoder) {
    this.repositorioUsuario = repositorioUsuario;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Usuario consultarUsuario(String email, String password) {
    Usuario usuario = repositorioUsuario.buscar(email);
    if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
      return usuario;
    }
    return null;
  }
}
