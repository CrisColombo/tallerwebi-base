package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Especie;
import com.tallerwebi.dominio.FiltroCobertura;
import com.tallerwebi.dominio.ItemCobertura;
import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.ServicioCobertura;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorServicioTest {

  private ControladorServicio controlador;
  private ServicioSocio servicioSocio;
  private ServicioCobertura servicioCobertura;
  private HttpSession sesion;
  private Socio socio;
  private Mascota luna;
  private Mascota toto;
  private List<ItemCobertura> items;

  @BeforeEach
  public void init() {
    servicioSocio = mock(ServicioSocio.class);
    servicioCobertura = mock(ServicioCobertura.class);
    sesion = mock(HttpSession.class);
    controlador = new ControladorServicio(servicioSocio, servicioCobertura);
    socio = new Socio();
    luna = mascota(1L, "Luna");
    toto = mascota(2L, "Toto");
    socio.agregarMascota(luna);
    socio.agregarMascota(toto);
    items = List.of(mock(ItemCobertura.class));
    when(
      servicioCobertura.coberturaDe(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
      )
    )
      .thenReturn(items);
  }

  @Test
  public void sinSocioDeberiaListarServiciosVacios() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(null);

    ModelAndView vista = controlador.listarServicios(null, FiltroCobertura.TODOS, sesion);

    assertThat(vista.getViewName(), equalTo("servicios"));
    assertThat(vista.getModel().get("mascota"), nullValue());
    assertThat(vista.getModel().get("mascotas"), equalTo(List.of()));
    verify(servicioCobertura).coberturaDe(null, FiltroCobertura.TODOS);
  }

  @Test
  public void conMascotaIdValidoDeberiaUsarEsaMascota() {
    loguearSocio();

    ModelAndView vista = controlador.listarServicios(2L, FiltroCobertura.INCLUIDOS, sesion);

    assertThat(vista.getModel().get("mascota"), equalTo(toto));
    assertThat(vista.getModel().get("filtro"), equalTo(FiltroCobertura.INCLUIDOS));
    assertThat(vista.getModel().get("items"), equalTo(items));
    verify(servicioCobertura).coberturaDe(toto, FiltroCobertura.INCLUIDOS);
  }

  @Test
  public void siElIdNoExisteDeberiaUsarLaPrimeraMascota() {
    loguearSocio();

    ModelAndView vista = controlador.listarServicios(99L, FiltroCobertura.NO_INCLUIDOS, sesion);

    assertThat(vista.getModel().get("mascota"), equalTo(luna));
    verify(servicioCobertura).coberturaDe(luna, FiltroCobertura.NO_INCLUIDOS);
  }

  @Test
  public void sinMascotaIdDeberiaUsarLaPrimeraMascota() {
    loguearSocio();

    ModelAndView vista = controlador.listarServicios(null, FiltroCobertura.TODOS, sesion);

    assertThat(vista.getModel().get("mascota"), equalTo(luna));
  }

  private void loguearSocio() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(8L);
    when(servicioSocio.buscarPorUsuario(8L)).thenReturn(socio);
  }

  private Mascota mascota(Long id, String nombre) {
    Mascota mascota = new Mascota();
    mascota.setId(id);
    mascota.setNombre(nombre);
    mascota.setEspecie(Especie.GATO);
    return mascota;
  }
}
