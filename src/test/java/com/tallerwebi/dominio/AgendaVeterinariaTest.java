package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgendaVeterinariaTest {

  // "Ahora" es el jueves 12/11/2026 a las 12:00.
  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
  private static final LocalDateTime AHORA = HOY.atTime(LocalTime.NOON);

  private Mascota mora;
  private Mascota toby;

  @BeforeEach
  public void init() {
    mora = dadaUnaMascota("Mora", "Carla", "Benítez");
    toby = dadaUnaMascota("Toby", "Martín", "Ruiz");
  }

  @Test
  public void hoyDeberiaMostrarSoloLosTurnosDeHoyOrdenadosPorHora() {
    Turno tarde = dadoUnTurno(mora, HOY, 18, EstadoTurno.CONFIRMADO);
    Turno manana = dadoUnTurno(mora, HOY, 9, EstadoTurno.FINALIZADO);
    Turno otroDia = dadoUnTurno(mora, HOY.plusDays(1), 10, EstadoTurno.CONFIRMADO);

    AgendaVeterinaria agenda = new AgendaVeterinaria(
      List.of(tarde, manana, otroDia),
      "hoy",
      null,
      AHORA
    );

    assertThat(agenda.getTurnos(), contains(manana, tarde));
    assertThat(agenda.getAtendidos(), is(1L));
    assertThat(agenda.getSiguiente(), is(tarde));
    assertThat(agenda.getHoy(), is(HOY));
  }

  @Test
  public void mananaYSemanaDeberianMostrarSuPeriodo() {
    Turno deManana = dadoUnTurno(mora, HOY.plusDays(1), 10, EstadoTurno.CONFIRMADO);
    Turno enSeisDias = dadoUnTurno(mora, HOY.plusDays(6), 10, EstadoTurno.CONFIRMADO);
    Turno enSieteDias = dadoUnTurno(mora, HOY.plusDays(7), 10, EstadoTurno.CONFIRMADO);
    List<Turno> todos = List.of(deManana, enSeisDias, enSieteDias);

    assertThat(new AgendaVeterinaria(todos, "manana", null, AHORA).getTurnos(), contains(deManana));
    assertThat(
      new AgendaVeterinaria(todos, "semana", null, AHORA).getTurnos(),
      contains(deManana, enSeisDias)
    );
    assertThat(
      new AgendaVeterinaria(todos, "todos", null, AHORA).getTurnos(),
      contains(enSieteDias, enSeisDias, deManana)
    );
  }

  @Test
  public void losTurnosConfirmadosDeDiasAnterioresDeberianQuedarSinCerrar() {
    Turno olvidado = dadoUnTurno(mora, HOY.minusDays(2), 10, EstadoTurno.CONFIRMADO);
    Turno cerrado = dadoUnTurno(mora, HOY.minusDays(3), 10, EstadoTurno.FINALIZADO);
    Turno deHoy = dadoUnTurno(mora, HOY, 9, EstadoTurno.CONFIRMADO);

    AgendaVeterinaria agenda = new AgendaVeterinaria(
      List.of(olvidado, cerrado, deHoy),
      "hoy",
      "",
      AHORA
    );

    assertThat(agenda.getSinCerrar(), contains(olvidado));
    assertThat(agenda.getSiguiente(), is(nullValue()));
  }

  @Test
  public void laBusquedaDeberiaMirarLaMascotaYElSocio() {
    Turno deMora = dadoUnTurno(mora, HOY, 10, EstadoTurno.CONFIRMADO);
    Turno deToby = dadoUnTurno(toby, HOY, 11, EstadoTurno.CONFIRMADO);
    List<Turno> todos = List.of(deMora, deToby);

    assertThat(new AgendaVeterinaria(todos, "hoy", "mora", AHORA).getTurnos(), contains(deMora));
    assertThat(new AgendaVeterinaria(todos, "hoy", " RUIZ ", AHORA).getTurnos(), contains(deToby));
  }

  @Test
  public void unPeriodoDesconocidoDeberiaMostrarHoy() {
    AgendaVeterinaria agenda = new AgendaVeterinaria(List.of(), "cualquiera", null, AHORA);

    assertThat(agenda.getPeriodo(), is(AgendaVeterinaria.HOY));
  }

  @Test
  public void unaMascotaSinSocioNoDeberiaRomperLaBusqueda() {
    Mascota sinSocio = new Mascota();
    sinSocio.setNombre("Coco");
    Turno turno = dadoUnTurno(sinSocio, HOY, 10, EstadoTurno.CONFIRMADO);

    assertThat(
      new AgendaVeterinaria(List.of(turno), "hoy", "coco", AHORA).getTurnos(),
      contains(turno)
    );
  }

  private Mascota dadaUnaMascota(String nombre, String nombreSocio, String apellidoSocio) {
    Socio socio = new Socio();
    socio.setNombre(nombreSocio);
    socio.setApellido(apellidoSocio);
    Mascota mascota = new Mascota();
    mascota.setNombre(nombre);
    socio.agregarMascota(mascota);
    return mascota;
  }

  private Turno dadoUnTurno(Mascota mascota, LocalDate fecha, int hora, EstadoTurno estado) {
    Turno turno = new Turno();
    turno.setMascota(mascota);
    turno.setFecha(fecha);
    turno.setHora(LocalTime.of(hora, 0));
    turno.setEstado(estado);
    return turno;
  }
}
