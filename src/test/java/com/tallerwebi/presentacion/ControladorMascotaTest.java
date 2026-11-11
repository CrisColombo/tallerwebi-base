package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Especie;
import com.tallerwebi.dominio.HistorialTurnos;
import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.Plan;
import com.tallerwebi.dominio.RecetasDeConsulta;
import com.tallerwebi.dominio.ServicioMascota;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPlan;
import com.tallerwebi.dominio.ServicioReceta;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.ServicioTurno;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.DatosDeRegistroInvalidos;
import com.tallerwebi.dominio.excepcion.PagoRechazado;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorMascotaTest {

  private ControladorMascota controladorMascota;
  private ServicioSocio servicioSocio;
  private ServicioPlan servicioPlan;
  private ServicioMascota servicioMascota;
  private ServicioTurno servicioTurno;
  private ServicioPago servicioPago;
  private ServicioReceta servicioReceta;
  private HttpSession sesion;
  private Socio socio;
  private Mascota mascota;
  private Mascota firulais;

  @BeforeEach
  public void init() {
    servicioSocio = mock(ServicioSocio.class);
    servicioPlan = mock(ServicioPlan.class);
    servicioMascota = mock(ServicioMascota.class);
    servicioTurno = mock(ServicioTurno.class);
    servicioPago = mock(ServicioPago.class);
    servicioReceta = mock(ServicioReceta.class);
    controladorMascota =
      new ControladorMascota(
        servicioSocio,
        servicioPlan,
        servicioMascota,
        servicioTurno,
        servicioPago,
        servicioReceta
      );
    sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    socio = new Socio();
    socio.setUsuario(new Usuario());
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(socio);
    when(servicioPlan.listar()).thenReturn(List.of(new Plan()));

    // mascota nueva, para los tests de "agregar mascota"
    mascota = new Mascota();
    mascota.setNombre("Luna");
    mascota.setEspecie(Especie.GATO);

    // mascota existente del socio, para los tests del perfil e historial
    firulais = new Mascota();
    firulais.setId(1L);
    socio.agregarMascota(firulais);
    when(servicioMascota.buscarPorIdParaSocio(socio, 1L)).thenReturn(firulais);
  }

  // ---------- Agregar mascota ----------

  @Test
  public void nuevaMascotaDeberiaMostrarFormularioConEspeciesYPlanes() {
    ModelAndView mav = controladorMascota.nuevaMascota(sesion);

    assertThat(mav.getViewName(), equalTo("nueva-mascota"));
    assertThat(mav.getModel().get("mascota"), instanceOf(Mascota.class));
    assertThat(mav.getModel().get("especies"), is(Especie.values()));
    assertThat(mav.getModel().get("planes"), is(servicioPlan.listar()));
  }

  @Test
  public void agregarMascotaDeberiaGuardarYRedirigirAMercadoPago() throws Exception {
    mascota.setId(7L);
    when(servicioMascota.agregarMascota(socio, mascota, 2L)).thenReturn(mascota);
    when(servicioPago.iniciarPagoPlan(socio, 7L, 2L))
      .thenReturn("https://sandbox.mercadopago.com/checkout");

    ModelAndView mav = controladorMascota.agregarMascota(mascota, 2L, sesion);

    verify(servicioMascota).agregarMascota(socio, mascota, 2L);
    assertThat(mav.getViewName(), equalTo("redirect:https://sandbox.mercadopago.com/checkout"));
  }

  @Test
  public void agregarMascotaSiMercadoPagoFallaDeberiaVolverAlPerfil() throws Exception {
    mascota.setId(7L);
    when(servicioMascota.agregarMascota(socio, mascota, 2L)).thenReturn(mascota);
    when(servicioPago.iniciarPagoPlan(socio, 7L, 2L)).thenThrow(new PagoRechazado("Plan inválido"));

    ModelAndView mav = controladorMascota.agregarMascota(mascota, 2L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/mascotas/7?errorPago=Plan+inv%C3%A1lido"));
  }

  @Test
  public void agregarMascotaConErrorDeValidacionDeberiaConservarDatos() throws Exception {
    doThrow(new DatosDeRegistroInvalidos("Elegí un plan para tu mascota"))
      .when(servicioMascota)
      .agregarMascota(socio, mascota, 2L);

    ModelAndView mav = controladorMascota.agregarMascota(mascota, 2L, sesion);

    assertThat(mav.getViewName(), equalTo("nueva-mascota"));
    assertThat(mav.getModel().get("mascota"), is(mascota));
    assertThat(mav.getModel().get("planId"), is(2L));
    assertThat(mav.getModel().get("error"), equalTo("Elegí un plan para tu mascota"));
  }

  // ---------- Perfil e historial de turnos ----------

  @Test
  public void verPerfilDeberiaMostrarElHistorialDeTurnosDeLaMascota() throws TurnoInvalido {
    HistorialTurnos historial = new HistorialTurnos(List.of(), List.of());
    when(servicioTurno.historialDe(socio, 1L)).thenReturn(historial);

    ModelAndView mav = controladorMascota.verPerfil(1L, sesion);

    assertThat(mav.getViewName(), equalTo("mascota-perfil"));
    assertThat(mav.getModel().get("historial"), is(historial));
  }

  @Test
  public void verPerfilDeUnaMascotaAjenaDeberiaVolverAlHome() throws TurnoInvalido {
    ModelAndView mav = controladorMascota.verPerfil(999L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verify(servicioTurno, never()).historialDe(any(), any());
  }

  @Test
  public void verPerfilSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(null);

    ModelAndView mav = controladorMascota.verPerfil(1L, sesion);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void verPerfilDeberiaMostrarLasRecetasDeLaMascota() throws TurnoInvalido {
    List<RecetasDeConsulta> recetas = List.of(new RecetasDeConsulta(new Turno()));
    when(servicioReceta.recetasDe(socio, 1L)).thenReturn(recetas);

    ModelAndView mav = controladorMascota.verPerfil(1L, sesion);

    assertThat(mav.getModel().get("recetas"), is(recetas));
  }

  @Test
  public void marcarRecetaCompradaDeberiaVolverAlPerfilConMensaje() throws TurnoInvalido {
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controladorMascota.marcarRecetaComprada(1L, 5L, sesion, attrs);

    verify(servicioReceta).marcarComprada(socio, 5L);
    assertThat(mav.getViewName(), equalTo("redirect:/mascotas/1"));
    assertThat(
      attrs.getFlashAttributes().get("mensaje"),
      equalTo("Marcaste la receta como comprada.")
    );
  }

  @Test
  public void marcarRecetaInvalidaDeberiaVolverAlPerfilConElError() throws TurnoInvalido {
    doThrow(new TurnoInvalido("No encontramos esa receta"))
      .when(servicioReceta)
      .marcarComprada(socio, 5L);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controladorMascota.marcarRecetaComprada(1L, 5L, sesion, attrs);

    assertThat(mav.getViewName(), equalTo("redirect:/mascotas/1"));
    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos esa receta"));
  }

  @Test
  public void marcarRecetaCompradaSinSerSocioDeberiaVolverAlHome() throws TurnoInvalido {
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(null);

    ModelAndView mav = controladorMascota.marcarRecetaComprada(
      1L,
      5L,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioReceta);
  }
}
