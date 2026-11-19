package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.*;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorTurnoTest {

  private ControladorTurno controladorTurno;
  private ServicioSocio servicioSocioMock;
  private ServicioTurno servicioTurnoMock;
  private ServicioResena servicioResena;
  private ServicioReceta servicioRecetaMock;
  private HttpSession sesionMock;
  private Socio socio;
  private SolicitudTurno solicitud;
  private RedirectAttributesModelMap redirectAttributes;

  @BeforeEach
  public void init() {
    servicioSocioMock = mock(ServicioSocio.class);
    servicioTurnoMock = mock(ServicioTurno.class);
    servicioResena = mock(ServicioResena.class);
    servicioRecetaMock = mock(ServicioReceta.class);
    controladorTurno =
      new ControladorTurno(
        servicioSocioMock,
        servicioTurnoMock,
        mock(ServicioVeterinaria.class),
        mock(ServicioCobertura.class),
        servicioResena,
        servicioRecetaMock
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
    ModelAndView mav = controladorTurno.misTurnos(new FiltroTurnos(), sesionMock);

    assertThat(mav.getViewName(), equalTo("mis-turnos"));
    verify(servicioTurnoMock, times(1)).turnosDe(socio);
  }

  @Test
  public void misTurnosDeberiaAplicarElFiltroYOfrecerLasOpcionesDeLosTurnos() {
    Mascota michi = new Mascota();
    michi.setId(2L);
    michi.setNombre("Michi");
    socio.agregarMascota(michi);
    Veterinaria veterinaria = new Veterinaria();
    veterinaria.setId(1L);
    veterinaria.setNombre("San Justo");
    Servicio servicio = new Servicio();
    servicio.setId(1L);
    servicio.setNombre("Consulta clínica");
    Turno atendido = new Turno();
    atendido.setId(7L);
    atendido.setMascota(michi);
    atendido.setVeterinaria(veterinaria);
    atendido.setServicio(servicio);
    atendido.setFecha(LocalDate.now().minusDays(3));
    atendido.setHora(LocalTime.of(10, 0));
    atendido.finalizar("Otitis leve");
    when(servicioTurnoMock.turnosDe(socio)).thenReturn(List.of(atendido));
    FiltroTurnos filtro = new FiltroTurnos();
    filtro.setVista(FiltroTurnos.ATENDIDOS);

    ModelAndView mav = controladorTurno.misTurnos(filtro, sesionMock);

    assertThat(mav.getModel().get("turnos"), is(List.of(atendido)));
    assertThat(mav.getModel().get("veterinarias"), is(List.of(veterinaria)));
    assertThat(mav.getModel().get("servicios"), is(List.of(servicio)));
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
  public void reservarSinSerSocioDeberiaVolverAlHome() throws TurnoInvalido {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.reservar(solicitud, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verify(servicioTurnoMock, never()).reservar(any(), any());
  }

  @Test
  public void misTurnosSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.misTurnos(new FiltroTurnos(), sesionMock);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void cancelarSinSerSocioDeberiaVolverAlHome() throws TurnoInvalido {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.cancelar(5L, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
    verify(servicioTurnoMock, never()).cancelar(any(), any());
  }

  @Test
  public void formularioReprogramarSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.formularioReprogramar(
      5L,
      LocalDate.of(2026, 10, 2),
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }

  @Test
  public void formularioReprogramarDeUnTurnoInvalidoDeberiaVolverAMisTurnos() throws TurnoInvalido {
    when(servicioTurnoMock.buscarTurnoDe(socio, 5L))
      .thenThrow(new TurnoInvalido("No encontramos ese turno"));

    ModelAndView mav = controladorTurno.formularioReprogramar(
      5L,
      LocalDate.of(2026, 10, 2),
      sesionMock,
      redirectAttributes
    );

    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("error"),
      equalTo("No encontramos ese turno")
    );
  }

  @Test
  public void misTurnosDeberiaMarcarSiElSocioYaResenoUnTurnoFinalizado() {
    Turno finalizado = new Turno();
    finalizado.setId(8L);
    finalizado.finalizar();
    when(servicioTurnoMock.turnosDe(socio)).thenReturn(List.of(finalizado));
    when(servicioResena.socioYaReseno(8L)).thenReturn(true);

    ModelAndView mav = controladorTurno.misTurnos(new FiltroTurnos(), sesionMock);

    assertThat(mav.getModel().get("resenasPorTurno"), equalTo(java.util.Map.of(8L, true)));
  }

  @Test
  public void nuevoTurnoDeUnaMascotaSinPlanVigenteDeberiaAvisarlo() {
    Mascota luna = new Mascota();
    luna.setId(3L);
    socio.agregarMascota(luna);
    solicitud.setMascotaId(3L);

    ModelAndView mav = controladorTurno.nuevoTurno(solicitud, sesionMock);

    assertThat(mav.getModel().get("sinPlanVigente"), equalTo(true));
  }

  @Test
  public void detalleDeberiaMostrarElTurnoDelSocioConSusRecetas() throws TurnoInvalido {
    Turno turno = new Turno();
    turno.setId(5L);
    turno.finalizar("Reposo");
    List<Receta> recetas = List.of(new Receta(turno, "Otomax", "2 gotas"));
    when(servicioTurnoMock.buscarTurnoDe(socio, 5L)).thenReturn(turno);
    when(servicioRecetaMock.recetasDelTurno(turno)).thenReturn(recetas);
    when(servicioResena.socioYaReseno(5L)).thenReturn(false);

    ModelAndView mav = controladorTurno.detalle(5L, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("detalle-turno"));
    assertThat(mav.getModel().get("turno"), is(turno));
    assertThat(mav.getModel().get("recetas"), is(recetas));
    assertThat(mav.getModel().get("esVeterinaria"), is(false));
    assertThat(mav.getModel().get("resenado"), is(false));
  }

  @Test
  public void detalleDeUnTurnoAjenoDeberiaVolverAMisTurnosConError() throws TurnoInvalido {
    when(servicioTurnoMock.buscarTurnoDe(socio, 99L))
      .thenThrow(new TurnoInvalido("No encontramos ese turno"));

    ModelAndView mav = controladorTurno.detalle(99L, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("redirect:/turnos"));
    assertThat(
      redirectAttributes.getFlashAttributes().get("error"),
      equalTo("No encontramos ese turno")
    );
  }

  @Test
  public void detalleSinSerSocioDeberiaVolverAlHome() {
    when(servicioSocioMock.buscarPorUsuario(2L)).thenReturn(null);

    ModelAndView mav = controladorTurno.detalle(5L, sesionMock, redirectAttributes);

    assertThat(mav.getViewName(), equalTo("redirect:/home"));
  }
}
