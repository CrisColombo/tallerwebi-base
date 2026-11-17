package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPendientesTest {

  // "Ahora" es el jueves 12/11/2026 a las 12:00.
  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

  private RepositorioTurno repositorioTurnoMock;
  private RepositorioReceta repositorioRecetaMock;
  private ServicioResena servicioResenaMock;
  private ServicioPendientes servicioPendientes;
  private Socio socio;
  private Mascota michi;

  @BeforeEach
  public void init() {
    repositorioTurnoMock = mock(RepositorioTurno.class);
    repositorioRecetaMock = mock(RepositorioReceta.class);
    servicioResenaMock = mock(ServicioResena.class);
    Clock reloj = Clock.fixed(
      HOY.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
      ZoneId.systemDefault()
    );
    servicioPendientes =
      new ServicioPendientesImpl(
        repositorioTurnoMock,
        repositorioRecetaMock,
        servicioResenaMock,
        reloj
      );
    socio = new Socio();
    socio.setId(1L);
    michi = new Mascota();
    michi.setId(2L);
    socio.agregarMascota(michi);
    when(repositorioRecetaMock.listarPorMascota(2L)).thenReturn(List.of());
  }

  @Test
  public void losProximosTurnosDeberianVenirOrdenadosYSinLosQueYaPasaron() {
    Turno tarde = dadoUnTurno(1L, HOY.plusDays(7), EstadoTurno.CONFIRMADO);
    Turno pronto = dadoUnTurno(2L, HOY.plusDays(2), EstadoTurno.CONFIRMADO);
    Turno yaPaso = dadoUnTurno(3L, HOY.minusDays(1), EstadoTurno.CONFIRMADO);
    Turno cancelado = dadoUnTurno(4L, HOY.plusDays(3), EstadoTurno.CANCELADO);
    dadoQueElSocioTiene(tarde, pronto, yaPaso, cancelado);

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getProximosTurnos(), contains(pronto, tarde));
    assertThat(pendientes.getProximoTurno(), is(pronto));
    assertThat(pendientes.proximoTurnoDe(michi), is(pronto));
  }

  @Test
  public void unaConsultaFinalizadaHacePocoDeberiaAparecerComoNovedadYNoRepetirseEnResenas() {
    Turno reciente = dadoUnTurno(1L, HOY.minusDays(3), EstadoTurno.FINALIZADO);
    Turno vieja = dadoUnTurno(2L, HOY.minusDays(40), EstadoTurno.FINALIZADO);
    dadoQueElSocioTiene(reciente, vieja);

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getConsultasRecientes(), contains(reciente));
    assertThat(pendientes.getTurnosSinResena(), contains(vieja));
  }

  @Test
  public void unaConsultaYaResenadaNoDeberiaQuedarPendiente() {
    Turno resenado = dadoUnTurno(1L, HOY.minusDays(2), EstadoTurno.FINALIZADO);
    dadoQueElSocioTiene(resenado);
    when(servicioResenaMock.socioYaReseno(1L)).thenReturn(true);

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getConsultasRecientes(), is(empty()));
    assertThat(pendientes.getTurnosSinResena(), is(empty()));
    assertThat(pendientes.isVacio(), is(true));
  }

  @Test
  public void unaConsultaDeHaceMasDeDosMesesNoDeberiaPedirResena() {
    Turno muyVieja = dadoUnTurno(1L, HOY.minusDays(61), EstadoTurno.FINALIZADO);
    dadoQueElSocioTiene(muyVieja);

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getTurnosSinResena(), is(empty()));
  }

  @Test
  public void soloLasRecetasSinComprarDeberianQuedarPendientes() {
    Turno consulta = dadoUnTurno(1L, HOY.minusDays(30), EstadoTurno.FINALIZADO);
    Receta pendiente = new Receta(consulta, "Otomax", "4 gotas cada 12 h");
    Receta comprada = new Receta(consulta, "Meloxicam", "");
    comprada.marcarComprada(HOY.minusDays(1));
    dadoQueElSocioTiene(consulta);
    when(servicioResenaMock.socioYaReseno(1L)).thenReturn(true);
    when(repositorioRecetaMock.listarPorMascota(2L)).thenReturn(List.of(pendiente, comprada));

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getRecetasSinComprar(), contains(pendiente));
    assertThat(pendientes.getCantidad(), is(1));
  }

  @Test
  public void unPlanQueVenceEnPocosDiasOSinPagoDeberiaAvisarse() {
    michi.setPlan(new Plan());
    michi.setVenceSuscripcion(HOY.plusDays(4));
    Mascota sinPago = new Mascota();
    sinPago.setId(3L);
    sinPago.setPlan(new Plan());
    Mascota alDia = new Mascota();
    alDia.setId(4L);
    alDia.setPlan(new Plan());
    alDia.setVenceSuscripcion(HOY.plusDays(20));
    Mascota sinPlan = new Mascota();
    sinPlan.setId(5L);
    socio.agregarMascota(sinPago);
    socio.agregarMascota(alDia);
    socio.agregarMascota(sinPlan);
    dadoQueElSocioTiene();

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getPlanesPorVencer(), contains(michi, sinPago));
  }

  @Test
  public void sinTurnosNoDeberiaHaberProximoTurno() {
    dadoQueElSocioTiene();

    Pendientes pendientes = servicioPendientes.pendientesDe(socio);

    assertThat(pendientes.getProximoTurno(), is(nullValue()));
    assertThat(pendientes.proximoTurnoDe(michi), is(nullValue()));
  }

  private void dadoQueElSocioTiene(Turno... turnos) {
    when(repositorioTurnoMock.listarPorSocio(1L)).thenReturn(List.of(turnos));
  }

  private Turno dadoUnTurno(Long id, LocalDate fecha, EstadoTurno estado) {
    Turno turno = new Turno();
    turno.setId(id);
    turno.setMascota(michi);
    turno.setFecha(fecha);
    turno.setHora(LocalTime.of(10, 0));
    turno.setEstado(estado);
    return turno;
  }
}
