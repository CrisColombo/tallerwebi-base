package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.ServicioCobertura;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioVeterinaria;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Veterinaria;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorVeterinariaTest {

  private ServicioVeterinaria servicioVeterinaria;
  private ServicioCobertura servicioCobertura;
  private ServicioResena servicioResena;
  private ControladorVeterinaria controlador;

  @BeforeEach
  public void init() {
    servicioVeterinaria = mock(ServicioVeterinaria.class);
    servicioCobertura = mock(ServicioCobertura.class);
    servicioResena = mock(ServicioResena.class);
    controlador =
      new ControladorVeterinaria(servicioVeterinaria, servicioCobertura, servicioResena);
  }

  @Test
  public void listarDeberiaMostrarVeterinariasParaSocios() {
    List<Veterinaria> veterinarias = List.of(new Veterinaria());
    HttpSession sesion = mock(HttpSession.class);
    when(servicioVeterinaria.listar(4L)).thenReturn(veterinarias);
    when(servicioCobertura.listarServicios()).thenReturn(List.of());
    when(servicioResena.resumenesPorVeterinarias(veterinarias)).thenReturn(Map.of());

    ModelAndView mav = controlador.listarVeterinarias(4L, sesion);

    assertThat(mav.getViewName(), equalTo("veterinarias"));
    assertThat(mav.getModel().get("veterinarias"), is(veterinarias));
    assertThat(mav.getModel().get("resenas"), is(Map.of()));
    verify(servicioVeterinaria).listar(4L);
    verify(servicioResena).resumenesPorVeterinarias(veterinarias);
  }

  @Test
  public void veterinariaNoDeberiaPoderVerElListado() {
    HttpSession sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn(Usuario.ROL_VETERINARIA);

    ModelAndView mav = controlador.listarVeterinarias(null, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/inicio"));
    verifyNoInteractions(servicioVeterinaria, servicioCobertura);
  }
}
