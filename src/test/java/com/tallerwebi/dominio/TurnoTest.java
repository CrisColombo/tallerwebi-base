package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

public class TurnoTest {

  @Test
  public void unTurnoNuevoNoDeberiaTenerElRecordatorioEnviado() {
    assertThat(new Turno().isRecordatorioEnviado(), is(false));
  }

  @Test
  public void marcarElRecordatorioEnviadoDeberiaDejarloEnviado() {
    Turno turno = new Turno();

    turno.marcarRecordatorioEnviado();

    assertThat(turno.isRecordatorioEnviado(), is(true));
  }

  @Test
  public void reprogramarUnTurnoDeberiaVolverAPermitirSuRecordatorio() {
    Turno turno = new Turno();
    turno.marcarRecordatorioEnviado();

    turno.reprogramar(LocalDate.of(2026, 10, 5), LocalTime.of(10, 0));

    assertThat(turno.isRecordatorioEnviado(), is(false));
  }
}
