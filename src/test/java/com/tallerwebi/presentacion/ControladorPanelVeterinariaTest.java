package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.DatosReceta;
import com.tallerwebi.dominio.RecetasDeConsulta;
import com.tallerwebi.dominio.ServicioCobertura;
import com.tallerwebi.dominio.ServicioReceta;
import com.tallerwebi.dominio.ServicioTurno;
import com.tallerwebi.dominio.ServicioVeterinaria;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Veterinaria;
import com.tallerwebi.dominio.excepcion.DatosDeRegistroInvalidos;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorPanelVeterinariaTest {

  private static final Long USUARIO_ID = 55L;
  private static final Long TURNO_ID = 9L;

  private ServicioVeterinaria servicioVeterinaria;
  private ServicioCobertura servicioCobertura;
  private ServicioTurno servicioTurno;
  private ServicioReceta servicioReceta;
  private ControladorPanelVeterinaria controlador;
  private HttpSession sesion;
  private Veterinaria veterinaria;
  private DatosVeterinaria datos;

  @BeforeEach
  public void init() {
    servicioVeterinaria = mock(ServicioVeterinaria.class);
    servicioCobertura = mock(ServicioCobertura.class);
    servicioTurno = mock(ServicioTurno.class);
    servicioReceta = mock(ServicioReceta.class);
    controlador =
      new ControladorPanelVeterinaria(
        servicioVeterinaria,
        servicioCobertura,
        servicioTurno,
        servicioReceta
      );
    sesion = mock(HttpSession.class);
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(USUARIO_ID);
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn(Usuario.ROL_VETERINARIA);
    Usuario usuario = new Usuario();
    usuario.setId(USUARIO_ID);
    usuario.setEmail("vet@test.com");
    veterinaria = new Veterinaria();
    veterinaria.setId(7L);
    veterinaria.setUsuario(usuario);
    when(servicioVeterinaria.buscarPorUsuario(USUARIO_ID)).thenReturn(veterinaria);
    when(servicioCobertura.listarServicios()).thenReturn(List.of());
    datos = new DatosVeterinaria();
    datos.setNombre("Veterinaria Central");
    datos.setCuit("30712345671");
    datos.setDireccion("Calle 123");
    datos.setTelefono("4444-1111");
    datos.setEmail("vet@test.com");
  }

  @Test
  public void panelDeberiaMostrarElPerfilEditable() {
    ModelAndView mav = controlador.panel(sesion);

    assertThat(mav.getViewName(), equalTo("panel-veterinaria"));
    assertThat(mav.getModel().get("datosVeterinaria"), instanceOf(DatosVeterinaria.class));
    assertThat(mav.getModel().get("veterinaria"), is(veterinaria));
  }

  @Test
  public void inicioDeberiaMostrarLaVeterinariaLogueada() {
    List<Turno> turnos = List.of(new Turno());
    when(servicioTurno.turnosDe(veterinaria)).thenReturn(turnos);

    ModelAndView mav = controlador.inicio(sesion);

    assertThat(mav.getViewName(), equalTo("inicio-veterinaria"));
    assertThat(mav.getModel().get("veterinaria"), is(veterinaria));
    assertThat(mav.getModel().get("turnos"), is(turnos));
  }

  @Test
  public void inicioDeberiaRechazarCuentasQueNoSonVeterinaria() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn("SOCIO");

    assertThat(controlador.inicio(sesion).getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioVeterinaria);
  }

  @Test
  public void panelDeberiaRechazarCuentasQueNoSonVeterinaria() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn("SOCIO");

    assertThat(controlador.panel(sesion).getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioVeterinaria);
  }

  @Test
  public void actualizarPerfilDeberiaGuardarCambiosYRedirigir() throws Exception {
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.actualizarPerfil(datos, sesion, attrs);

    verify(servicioVeterinaria)
      .actualizarPerfil(eq(USUARIO_ID), any(Veterinaria.class), eq("vet@test.com"), anyList());
    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/panel"));
    assertThat(
      attrs.getFlashAttributes().get("mensaje"),
      equalTo("Actualizaste los datos de tu veterinaria.")
    );
  }

  @Test
  public void actualizarPerfilConEmailExistenteDeberiaMostrarElError() throws Exception {
    doThrow(UsuarioExistente.class)
      .when(servicioVeterinaria)
      .actualizarPerfil(anyLong(), any(), anyString(), anyList());

    ModelAndView mav = controlador.actualizarPerfil(
      datos,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("panel-veterinaria"));
    assertThat(mav.getModel().get("error"), equalTo("Ya existe una cuenta con ese email"));
  }

  @Test
  public void actualizarPerfilConDatosInvalidosDeberiaMostrarElError() throws Exception {
    doThrow(new DatosDeRegistroInvalidos("Elegí al menos un servicio"))
      .when(servicioVeterinaria)
      .actualizarPerfil(anyLong(), any(), anyString(), anyList());

    ModelAndView mav = controlador.actualizarPerfil(
      datos,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getModel().get("error"), equalTo("Elegí al menos un servicio"));
  }

  @Test
  public void turnosDeberiaListarLosTurnosDeLaVeterinaria() {
    when(servicioTurno.turnosDe(veterinaria)).thenReturn(List.of(new Turno()));

    ModelAndView mav = controlador.turnos(sesion);

    assertThat(mav.getViewName(), equalTo("turnos-veterinaria"));
    assertThat(mav.getModel().get("turnos"), is(servicioTurno.turnosDe(veterinaria)));
  }

  @Test
  public void cancelarTurnoDeberiaRedirigirConMensaje() throws Exception {
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.cancelar(TURNO_ID, sesion, attrs);

    verify(servicioTurno).cancelarPorVeterinaria(veterinaria, TURNO_ID);
    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("mensaje"), equalTo("Cancelaste el turno."));
  }

  @Test
  public void cancelarTurnoInvalidoDeberiaMostrarElError() throws Exception {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurno)
      .cancelarPorVeterinaria(veterinaria, TURNO_ID);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    controlador.cancelar(TURNO_ID, sesion, attrs);

    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos ese turno"));
  }

  @Test
  public void formularioReprogramarDeberiaCargarElTurnoYHorarios() throws Exception {
    Turno turno = new Turno();
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);
    when(servicioTurno.horariosDisponibles(veterinaria, LocalDate.now()))
      .thenReturn(List.of(LocalTime.NOON));

    ModelAndView mav = controlador.formularioReprogramar(
      TURNO_ID,
      LocalDate.now(),
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("reprogramar-turno"));
    assertThat(mav.getModel().get("turno"), is(turno));
    assertThat(mav.getModel().get("esVeterinaria"), is(true));
    assertThat(mav.getModel().get("urlVolver"), equalTo("/veterinaria/turnos"));
  }

  @Test
  public void formularioReprogramarTurnoAjenoDeberiaVolverALista() throws Exception {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurno)
      .buscarTurnoDeVeterinaria(veterinaria, TURNO_ID);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.formularioReprogramar(TURNO_ID, null, sesion, attrs);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos ese turno"));
  }

  @Test
  public void reprogramarDeberiaGuardarLaNuevaFechaYHora() throws Exception {
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();
    LocalDate fecha = LocalDate.of(2026, 11, 2);
    LocalTime hora = LocalTime.of(10, 0);

    ModelAndView mav = controlador.reprogramar(TURNO_ID, fecha, hora, sesion, attrs);

    verify(servicioTurno).reprogramarPorVeterinaria(veterinaria, TURNO_ID, fecha, hora);
    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("mensaje"), equalTo("Reprogramaste el turno."));
  }

  @Test
  public void reprogramarTurnoInvalidoDeberiaVolverAlFormulario() throws Exception {
    doThrow(new TurnoInvalido("Horario ocupado"))
      .when(servicioTurno)
      .reprogramarPorVeterinaria(any(), anyLong(), any(), any());

    ModelAndView mav = controlador.reprogramar(
      TURNO_ID,
      LocalDate.of(2026, 11, 2),
      LocalTime.of(10, 0),
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(
      mav.getViewName(),
      equalTo("redirect:/veterinaria/turnos/" + TURNO_ID + "/reprogramar")
    );
  }

  @Test
  public void formularioFinalizarDeberiaCargarElTurno() throws Exception {
    Turno turno = new Turno();
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);

    ModelAndView mav = controlador.formularioFinalizar(
      TURNO_ID,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("finalizar-turno"));
    assertThat(mav.getModel().get("turno"), is(turno));
  }

  @Test
  public void formularioFinalizarTurnoAjenoDeberiaVolverALista() throws Exception {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurno)
      .buscarTurnoDeVeterinaria(veterinaria, TURNO_ID);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.formularioFinalizar(TURNO_ID, sesion, attrs);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos ese turno"));
  }

  @Test
  public void finalizarDeberiaFinalizarElTurnoYGuardarLasRecetas() throws Exception {
    Turno turno = new Turno();
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.finalizar(
      TURNO_ID,
      List.of("Amoxicilina", ""),
      List.of("1 cada 12hs", ""),
      "Otitis leve, control en 7 días",
      null,
      sesion,
      attrs
    );

    verify(servicioTurno).finalizar(veterinaria, TURNO_ID, "Otitis leve, control en 7 días", null);
    ArgumentCaptor<List<DatosReceta>> captor = ArgumentCaptor.forClass(List.class);
    verify(servicioReceta).registrar(eq(turno), captor.capture());
    assertThat(captor.getValue().size(), is(2));
    assertThat(captor.getValue().get(0).getMedicamento(), equalTo("Amoxicilina"));
    assertThat(captor.getValue().get(0).getIndicaciones(), equalTo("1 cada 12hs"));
    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("mensaje"), equalTo("Finalizaste la consulta."));
  }

  @Test
  public void finalizarSinMedicamentosDeberiaFinalizarIgual() throws Exception {
    Turno turno = new Turno();
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);

    controlador.finalizar(
      TURNO_ID,
      null,
      null,
      null,
      null,
      sesion,
      new RedirectAttributesModelMap()
    );

    verify(servicioTurno).finalizar(veterinaria, TURNO_ID, null, null);
    verify(servicioReceta).registrar(eq(turno), eq(List.of()));
  }

  @Test
  public void finalizarSinIndicacionesDeberiaGuardarElMedicamentoIgual() throws Exception {
    Turno turno = new Turno();
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);

    controlador.finalizar(
      TURNO_ID,
      List.of("Amoxicilina"),
      null,
      null,
      null,
      sesion,
      new RedirectAttributesModelMap()
    );

    ArgumentCaptor<List<DatosReceta>> captor = ArgumentCaptor.forClass(List.class);
    verify(servicioReceta).registrar(eq(turno), captor.capture());
    assertThat(captor.getValue().get(0).getIndicaciones(), equalTo(""));
  }

  @Test
  public void finalizarTurnoInvalidoDeberiaMostrarElErrorYNoGuardarRecetas() throws Exception {
    doThrow(new TurnoInvalido("Solo se pueden finalizar turnos confirmados"))
      .when(servicioTurno)
      .finalizar(veterinaria, TURNO_ID, null, null);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    controlador.finalizar(TURNO_ID, List.of("Amoxicilina"), List.of(""), null, null, sesion, attrs);

    assertThat(
      attrs.getFlashAttributes().get("error"),
      equalTo("Solo se pueden finalizar turnos confirmados")
    );
    verifyNoInteractions(servicioReceta);
  }

  @Test
  public void finalizarSinSesionDeVeterinariaDeberiaRedirigirAlHome() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn("SOCIO");

    ModelAndView mav = controlador.finalizar(
      TURNO_ID,
      null,
      null,
      null,
      null,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioReceta);
  }

  @Test
  public void medicacionAnteriorDeberiaMostrarLasRecetasDeLaMascota() throws Exception {
    Turno turno = new Turno();
    List<RecetasDeConsulta> consultas = List.of(new RecetasDeConsulta(new Turno()));
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);
    when(servicioReceta.medicacionAnterior(veterinaria, turno)).thenReturn(consultas);

    ModelAndView mav = controlador.medicacionAnterior(
      TURNO_ID,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("medicacion-anterior"));
    assertThat(mav.getModel().get("turno"), is(turno));
    assertThat(mav.getModel().get("consultas"), is(consultas));
  }

  @Test
  public void medicacionAnteriorDeUnTurnoAjenoDeberiaVolverALista() throws Exception {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurno)
      .buscarTurnoDeVeterinaria(veterinaria, TURNO_ID);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.medicacionAnterior(TURNO_ID, sesion, attrs);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos ese turno"));
  }

  @Test
  public void medicacionAnteriorSinSerVeterinariaDeberiaRedirigirAlHome() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn("SOCIO");

    ModelAndView mav = controlador.medicacionAnterior(
      TURNO_ID,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verifyNoInteractions(servicioReceta);
  }

  @Test
  public void historialClinicoDeberiaMostrarLasAtencionesDeLaMascota() throws Exception {
    Turno turno = new Turno();
    List<Turno> atenciones = List.of(new Turno());
    when(servicioTurno.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);
    when(servicioTurno.historialClinicoDe(veterinaria, TURNO_ID)).thenReturn(atenciones);

    ModelAndView mav = controlador.historialClinico(
      TURNO_ID,
      sesion,
      new RedirectAttributesModelMap()
    );

    assertThat(mav.getViewName(), equalTo("historial-mascota"));
    assertThat(mav.getModel().get("turno"), is(turno));
    assertThat(mav.getModel().get("atenciones"), is(atenciones));
  }

  @Test
  public void historialClinicoDeTurnoAjenoDeberiaVolverALista() throws Exception {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurno)
      .buscarTurnoDeVeterinaria(veterinaria, TURNO_ID);
    RedirectAttributesModelMap attrs = new RedirectAttributesModelMap();

    ModelAndView mav = controlador.historialClinico(TURNO_ID, sesion, attrs);

    assertThat(mav.getViewName(), equalTo("redirect:/veterinaria/turnos"));
    assertThat(attrs.getFlashAttributes().get("error"), equalTo("No encontramos ese turno"));
  }
}
