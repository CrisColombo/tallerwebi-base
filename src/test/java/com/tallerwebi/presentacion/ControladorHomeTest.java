package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.EstadoTurno;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.ServicioTurno;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.web.servlet.ModelAndView;

public class ControladorHomeTest {

  private ControladorHome controlador;
  private ServicioSocio servicioSocio;
  private ServicioTurno servicioTurno;
  private HttpSession sesion;
  private Socio socio;

  @BeforeEach
  public void init() {
    servicioSocio = mock(ServicioSocio.class);
    servicioTurno = mock(ServicioTurno.class);
    sesion = mock(HttpSession.class);
    controlador = new ControladorHome(servicioSocio, servicioTurno);
    socio = new Socio();
    socio.setId(1L);
  }

  @Test
  public void unaVeterinariaDeberiaIrASuInicio() {
    when(sesion.getAttribute(ControladorLogin.ROL)).thenReturn(Usuario.ROL_VETERINARIA);

    ModelAndView vista = controlador.irAHome(sesion);

    assertThat(vista.getViewName(), equalTo("redirect:/veterinaria/inicio"));
    verify(servicioTurno, never()).turnosDe(ArgumentMatchers.<Socio>any());
  }

  @Test
  public void elHomeDeberiaMostrarSoloTurnosConfirmadosDeHoyEnAdelanteOrdenados() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn(1L);
    when(servicioSocio.buscarPorUsuario(1L)).thenReturn(socio);
    Turno hoy = turno(EstadoTurno.CONFIRMADO, LocalDate.now(), LocalTime.of(18, 0));
    Turno manianaTemprano = turno(
      EstadoTurno.CONFIRMADO,
      LocalDate.now().plusDays(1),
      LocalTime.of(9, 0)
    );
    Turno manianaTarde = turno(
      EstadoTurno.CONFIRMADO,
      LocalDate.now().plusDays(1),
      LocalTime.of(15, 30)
    );
    Turno ayer = turno(EstadoTurno.CONFIRMADO, LocalDate.now().minusDays(1), LocalTime.of(10, 0));
    Turno cancelado = turno(
      EstadoTurno.CANCELADO,
      LocalDate.now().plusDays(2),
      LocalTime.of(11, 0)
    );
    when(servicioTurno.turnosDe(socio))
      .thenReturn(List.of(manianaTarde, ayer, cancelado, hoy, manianaTemprano));

    ModelAndView vista = controlador.irAHome(sesion);

    assertThat(vista.getViewName(), equalTo("home"));
    assertThat(vista.getModel().get("socio"), equalTo(socio));
    assertThat(proximosDe(vista), contains(hoy, manianaTemprano, manianaTarde));
  }

  @Test
  public void sinSocioElHomeNoDeberiaCargarTurnos() {
    when(sesion.getAttribute(ControladorLogin.USUARIO_ID)).thenReturn("admin");

    ModelAndView vista = controlador.irAHome(sesion);

    assertThat(vista.getViewName(), equalTo("home"));
    assertThat(vista.getModel().get("socio"), nullValue());
    assertThat(vista.getModel().get("proximosTurnos"), nullValue());
    verify(servicioTurno, never()).turnosDe(ArgumentMatchers.<Socio>any());
  }

  @SuppressWarnings("unchecked")
  private List<Turno> proximosDe(ModelAndView vista) {
    return (List<Turno>) vista.getModel().get("proximosTurnos");
  }

  private Turno turno(EstadoTurno estado, LocalDate fecha, LocalTime hora) {
    Turno turno = new Turno();
    turno.setEstado(estado);
    turno.setFecha(fecha);
    turno.setHora(hora);
    return turno;
  }
}
