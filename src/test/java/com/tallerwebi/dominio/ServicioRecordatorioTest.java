package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioRecordatorioTest {

  // "Ahora" es el miércoles 30/09/2026 a las 12:00.
  private static final LocalDate HOY = LocalDate.of(2026, 9, 30);
  private static final LocalDate MANANA = HOY.plusDays(1);

  private RepositorioTurno repositorioTurnoMock;
  private ServicioNotificacion servicioNotificacionMock;
  private ServicioRecordatorio servicioRecordatorio;

  @BeforeEach
  public void init() {
    repositorioTurnoMock = mock(RepositorioTurno.class);
    servicioNotificacionMock = mock(ServicioNotificacion.class);
    Clock reloj = Clock.fixed(
      LocalDateTime.of(HOY, LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
      ZoneId.systemDefault()
    );
    servicioRecordatorio =
      new ServicioRecordatorioImpl(repositorioTurnoMock, servicioNotificacionMock, reloj);
  }

  @Test
  public void unTurnoDentroDeLas24HorasDeberiaRecibirElRecordatorioYQuedarMarcado() {
    Turno turno = dadoUnTurno(1L, MANANA, LocalTime.of(10, 0), "socio@mail.com");
    dadoQueElRepositorioDevuelve(List.of(turno));

    servicioRecordatorio.enviarRecordatorios();

    verify(servicioNotificacionMock, times(1)).enviarRecordatorioTurno(turno, "socio@mail.com");
    assertThat(turno.isRecordatorioEnviado(), is(true));
  }

  @Test
  public void unTurnoJustoA24HorasDeberiaRecibirElRecordatorio() {
    Turno turno = dadoUnTurno(1L, MANANA, LocalTime.NOON, "socio@mail.com");
    dadoQueElRepositorioDevuelve(List.of(turno));

    servicioRecordatorio.enviarRecordatorios();

    verify(servicioNotificacionMock, times(1)).enviarRecordatorioTurno(turno, "socio@mail.com");
  }

  @Test
  public void unTurnoMasLejanoQue24HorasNoDeberiaRecibirElRecordatorio() {
    Turno turno = dadoUnTurno(1L, MANANA, LocalTime.of(12, 30), "socio@mail.com");
    dadoQueElRepositorioDevuelve(List.of(turno));

    servicioRecordatorio.enviarRecordatorios();

    verifyNoInteractions(servicioNotificacionMock);
    assertThat(turno.isRecordatorioEnviado(), is(false));
  }

  @Test
  public void unTurnoQueYaPasoNoDeberiaRecibirElRecordatorio() {
    Turno turno = dadoUnTurno(1L, HOY, LocalTime.of(9, 0), "socio@mail.com");
    dadoQueElRepositorioDevuelve(List.of(turno));

    servicioRecordatorio.enviarRecordatorios();

    verifyNoInteractions(servicioNotificacionMock);
    assertThat(turno.isRecordatorioEnviado(), is(false));
  }

  @Test
  public void siElSocioNoTieneMailNoDeberiaEnviarNadaNiMarcarElTurno() {
    Turno turno = dadoUnTurno(1L, MANANA, LocalTime.of(10, 0), " ");
    dadoQueElRepositorioDevuelve(List.of(turno));

    servicioRecordatorio.enviarRecordatorios();

    verifyNoInteractions(servicioNotificacionMock);
    assertThat(turno.isRecordatorioEnviado(), is(false));
  }

  @Test
  public void siElMailFallaElTurnoNoDeberiaQuedarMarcadoYSeguirConElSiguiente() {
    Turno primero = dadoUnTurno(1L, MANANA, LocalTime.of(9, 0), "uno@mail.com");
    Turno segundo = dadoUnTurno(2L, MANANA, LocalTime.of(10, 0), "dos@mail.com");
    dadoQueElRepositorioDevuelve(List.of(primero, segundo));
    doThrow(new IllegalStateException("SMTP caído"))
      .when(servicioNotificacionMock)
      .enviarRecordatorioTurno(eq(primero), anyString());

    servicioRecordatorio.enviarRecordatorios();

    assertThat(primero.isRecordatorioEnviado(), is(false));
    assertThat(segundo.isRecordatorioEnviado(), is(true));
    verify(servicioNotificacionMock, times(1)).enviarRecordatorioTurno(segundo, "dos@mail.com");
  }

  @Test
  public void sinTurnosPorRecordarNoDeberiaEnviarNada() {
    dadoQueElRepositorioDevuelve(List.of());

    servicioRecordatorio.enviarRecordatorios();

    verifyNoInteractions(servicioNotificacionMock);
  }

  // Se pide la lista de hoy a mañana: si el servicio usara otro rango, el mock devolvería vacío.
  private void dadoQueElRepositorioDevuelve(List<Turno> turnos) {
    when(repositorioTurnoMock.listarConfirmadosSinRecordatorio(HOY, MANANA)).thenReturn(turnos);
  }

  private Turno dadoUnTurno(Long id, LocalDate fecha, LocalTime hora, String email) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    Socio socio = new Socio();
    socio.setUsuario(usuario);
    Mascota mascota = new Mascota();
    socio.agregarMascota(mascota);

    Turno turno = new Turno();
    turno.setId(id);
    turno.setMascota(mascota);
    turno.setFecha(fecha);
    turno.setHora(hora);
    return turno;
  }
}
