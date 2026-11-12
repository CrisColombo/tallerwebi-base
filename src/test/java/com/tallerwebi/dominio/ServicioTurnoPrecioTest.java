package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioTurnoPrecioTest {

  private static final Long TURNO_ID = 5L;

  private RepositorioTurno repositorioTurnoMock;
  private ServicioTurno servicioTurno;
  private Veterinaria veterinaria;
  private Turno turno;

  @BeforeEach
  public void init() {
    repositorioTurnoMock = mock(RepositorioTurno.class);
    servicioTurno =
      new ServicioTurnoImpl(
        repositorioTurnoMock,
        mock(RepositorioVeterinaria.class),
        mock(RepositorioServicio.class),
        mock(ServicioNotificacion.class)
      );
    veterinaria = new Veterinaria();
    veterinaria.setId(1L);
    turno = new Turno();
    turno.setVeterinaria(veterinaria);
    when(repositorioTurnoMock.buscarPorId(TURNO_ID)).thenReturn(turno);
  }

  @Test
  public void finalizarDeberiaGuardarElPrecioParticularDeLaAtencion() throws Exception {
    servicioTurno.finalizar(veterinaria, TURNO_ID, null, 15000.0);

    assertThat(turno.getEstado(), is(EstadoTurno.FINALIZADO));
    assertThat(turno.getPrecioParticular(), equalTo(15000.0));
  }

  @Test
  public void finalizarSinPrecioDeberiaFinalizarIgual() throws Exception {
    servicioTurno.finalizar(veterinaria, TURNO_ID, null, null);

    assertThat(turno.getEstado(), is(EstadoTurno.FINALIZADO));
    assertThat(turno.getPrecioParticular(), is(nullValue()));
  }

  @Test
  public void noDeberiaFinalizarUnTurnoQueNoEstaConfirmado() {
    turno.cancelar();

    assertThrows(
      TurnoInvalido.class,
      () -> servicioTurno.finalizar(veterinaria, TURNO_ID, null, 15000.0)
    );
    assertThat(turno.getPrecioParticular(), is(nullValue()));
  }
}
