package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.*;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorTurnoTest {

  private ControladorTurno controladorTurno;
  private ServicioSocio servicioSocioMock;
  private ServicioTurno servicioTurnoMock;
  private HttpSession sesionMock;
  private Socio socio;
  private SolicitudTurno solicitud;
  private RedirectAttributesModelMap redirectAttributes;

  @BeforeEach
  public void init() {
    servicioSocioMock = mock(ServicioSocio.class);
    servicioTurnoMock = mock(ServicioTurno.class);
    controladorTurno =
      new ControladorTurno(
        servicioSocioMock,
        servicioTurnoMock,
        mock(ServicioVeterinaria.class),
        mock(ServicioCobertura.class),
        mock(ServicioResena.class)
      );

    sesionMock = mock(HttpSession.class);
    when(sesionMock.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(2L);
    socio = new Socio();
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(socio);
    solicitud = new SolicitudTurno();
    redirectAttributes = new RedirectAttributesModelMap();
  }

  @Test
  public void nuevoTurnoDeberiaMostrarElFormulario() {
    ModelAndView mav = controladorTurno.nuevoTurno(solicitud, sesionMock);

    assertThat(mav.getViewName(), equalTo("nuevo-turno"));
    assertThat(mav.getModel().get("solicitud"), is(solicitud));
  }

  @Test
  public void nuevoTurnoSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.nuevoTurno(solicitud, sesionMock);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void reservarConExitoDeberiaIrAMisTurnosConMensaje() throws TurnoInvalido {
    ModelAndView mav = controladorTurno.reservar(solicitud, sesionMock, redirectAttributes);

    verify(servicioTurnoMock, times(1)).reservar(socio, solicitud);
    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("mensaje"),
      equalTo("¡Listo! Tu turno quedó confirmado.")
    );
  }

  @Test
  public void reservarConErrorDeberiaVolverAlFormularioMostrandoElMotivo() throws TurnoInvalido {
    when(servicioTurnoMock.reservar(socio, solicitud))
      .thenThrow(new TurnoInvalido("El horario elegido ya no está disponible"));

    ModelAndView mav = controladorTurno.reservar(solicitud, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("nuevo-turno"));
    assertThat(mav.getModel().get("error"), equalTo("El horario elegido ya no está disponible"));
  }

  @Test
  public void misTurnosDeberiaMostrarLosTurnosDelSocio() {
    ModelAndView mav = controladorTurno.misTurnos(sesionMock);

    assertThat(mav.getViewName(), equalTo("mis-turnos"));
    verify(servicioTurnoMock, times(1)).turnosDe(socio);
  }

  @Test
  public void cancelarDeberiaCancelarElTurnoYVolverAMisTurnos() throws TurnoInvalido {
    ModelAndView mav = controladorTurno.cancelar(5L, sesionMock, redirectAttributes);

    verify(servicioTurnoMock, times(1)).cancelar(socio, 5L);
    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
  }

  @Test
  public void cancelarUnTurnoInvalidoDeberiaMostrarElError() throws TurnoInvalido {
    doThrow(new TurnoInvalido("No encontramos ese turno"))
      .when(servicioTurnoMock)
      .cancelar(socio, 5L);

    controladorTurno.cancelar(5L, sesionMock, redirectAttributes);

    assertThat(
      redirectAttributes.getFlashAttributes().get("error"),
      equalTo("No encontramos ese turno")
    );
  }

  @Test
  public void reprogramarConExitoDeberiaIrAMisTurnosConMensaje() throws TurnoInvalido {
    LocalDate fecha = LocalDate.of(2026, 10, 2);
    LocalTime hora = LocalTime.of(9, 30);

    ModelAndView mav = controladorTurno.reprogramar(
      5L,
      fecha,
      hora,
      sesionMock,
      redirectAttributes
    );

    verify(servicioTurnoMock, times(1)).reprogramar(socio, 5L, fecha, hora);
    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("mensaje"),
      equalTo("Reprogramaste tu turno.")
    );
  }

  @Test
  public void reprogramarConErrorDeberiaVolverAlFormularioMostrandoElMotivo() throws TurnoInvalido {
    LocalDate fecha = LocalDate.of(2026, 10, 2);
    LocalTime hora = LocalTime.of(9, 30);

    doThrow(new TurnoInvalido("El horario elegido ya no está disponible"))
      .when(servicioTurnoMock)
      .reprogramar(socio, 5L, fecha, hora);

    ModelAndView mav = controladorTurno.reprogramar(
      5L,
      fecha,
      hora,
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("redirect:/turnos/5/reprogramar"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("error"),
      equalTo("El horario elegido ya no está disponible")
    );
  }

  @Test
  public void reprogramarSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    LocalDate fecha = LocalDate.of(2026, 10, 2);
    LocalTime hora = LocalTime.of(9, 30);

    ModelAndView mav = controladorTurno.reprogramar(
      5L,
      fecha,
      hora,
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void formularioReprogramarDeberiaMostrarElTurnoYLosHorariosLibres() throws TurnoInvalido {
    Veterinaria veterinaria = new Veterinaria();
    Turno turno = new Turno();
    turno.setVeterinaria(veterinaria);
    when(servicioTurnoMock.buscarTurnoDe(socio, 5L)).thenReturn(turno);

    LocalDate fecha = LocalDate.of(2026, 10, 2);
    List<LocalTime> horarios = List.of(LocalTime.of(9, 0), LocalTime.of(9, 30));
    when(servicioTurnoMock.horariosDisponibles(veterinaria, fecha)).thenReturn(horarios);

    ModelAndView mav = controladorTurno.formularioReprogramar(
      5L,
      fecha,
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("reprogramar-turno"));
    assertThat(mav.getModel().get("turno"), is(turno));
    assertThat(mav.getModel().get("horarios"), is(horarios));
    assertThat(mav.getModel().get("fecha"), is(fecha));
  }

  @Test
  public void misTurnosDeberiaMostrarLosHorariosParaAdelantarCuandoSeLiberoUno() {
    Turno turno = new Turno();
    turno.setId(5L);
    LocalTime nueve = LocalTime.of(9, 0);
    when(servicioTurnoMock.turnosDe(socio)).thenReturn(List.of(turno));
    when(servicioTurnoMock.horariosParaAdelantar(turno)).thenReturn(List.of(nueve));

    ModelAndView mav = controladorTurno.misTurnos(sesionMock);

    assertThat(mav.getViewName(), equalTo("mis-turnos"));
    java.util.Map<?, ?> adelantos = (java.util.Map<?, ?>) mav.getModel().get("adelantosPorTurno");
    assertThat(adelantos.get(5L), equalTo(List.of(nueve)));
  }

  @Test
  public void misTurnosNoDeberiaMostrarAvisoSiNoHayHorariosParaAdelantar() {
    Turno turno = new Turno();
    turno.setId(5L);
    when(servicioTurnoMock.turnosDe(socio)).thenReturn(List.of(turno));
    when(servicioTurnoMock.horariosParaAdelantar(turno)).thenReturn(List.of());

    ModelAndView mav = controladorTurno.misTurnos(sesionMock);

    java.util.Map<?, ?> adelantos = (java.util.Map<?, ?>) mav.getModel().get("adelantosPorTurno");
    assertThat(adelantos.isEmpty(), is(true));
  }

  @Test
  public void adelantarConExitoDeberiaIrAMisTurnosConMensaje() throws TurnoInvalido {
    LocalTime hora = LocalTime.of(9, 0);

    ModelAndView mav = controladorTurno.adelantar(5L, hora, sesionMock, redirectAttributes);

    verify(servicioTurnoMock, times(1)).adelantar(socio, 5L, hora);
    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("mensaje"),
      equalTo("Adelantaste tu turno a las 09:00.")
    );
  }

  @Test
  public void adelantarConErrorDeberiaMostrarElMotivoYVolverAMisTurnos() throws TurnoInvalido {
    LocalTime hora = LocalTime.of(9, 0);
    doThrow(new TurnoInvalido("Ese horario ya no está disponible"))
      .when(servicioTurnoMock)
      .adelantar(socio, 5L, hora);

    ModelAndView mav = controladorTurno.adelantar(5L, hora, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("error"),
      equalTo("Ese horario ya no está disponible")
    );
  }

  @Test
  public void adelantarSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.adelantar(
      5L,
      LocalTime.of(9, 0),
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }
}
