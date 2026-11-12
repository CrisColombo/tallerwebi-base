package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import com.tallerwebi.infraestructura.RepositorioResena;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioResenaTest {

  private ServicioResena servicioResena;
  private RepositorioResena repositorioResena;
  private ServicioTurno servicioTurno;
  private Socio socio;
  private Turno turno;

  @BeforeEach
  public void init() throws TurnoInvalido {
    repositorioResena = mock(RepositorioResena.class);
    servicioTurno = mock(ServicioTurno.class);
    servicioResena = new ServicioResenaImpl(repositorioResena, servicioTurno);

    socio = new Socio();
    Veterinaria veterinaria = new Veterinaria();
    veterinaria.setId(1L);
    turno = new Turno();
    turno.setEstado(EstadoTurno.FINALIZADO);
    turno.setVeterinaria(veterinaria);

    when(servicioTurno.buscarTurnoDe(socio, 10L)).thenReturn(turno);
    when(repositorioResena.existePorTurno(10L)).thenReturn(false);
  }

  @Test
  public void crearResenaDeTurnoFinalizadoDeberiaGuardarla() throws ResenaInvalida {
    servicioResena.crearResena(socio, 10L, 5, "  Muy buena atención  ");

    verify(repositorioResena).guardar(any(Resena.class));
  }

  @Test
  public void puntuacionFueraDeRangoDeberiaFallar() {
    ResenaInvalida error = assertThrows(
      ResenaInvalida.class,
      () -> servicioResena.crearResena(socio, 10L, 0, "Bien")
    );

    assertThat(error.getMessage(), equalTo("La puntuación debe ser entre 1 y 5"));
  }

  @Test
  public void comentarioVacioDeberiaFallar() {
    ResenaInvalida error = assertThrows(
      ResenaInvalida.class,
      () -> servicioResena.crearResena(socio, 10L, 4, "   ")
    );

    assertThat(error.getMessage(), equalTo("Escribí un comentario"));
  }

  @Test
  public void turnoNoFinalizadoDeberiaFallar() {
    turno.setEstado(EstadoTurno.CONFIRMADO);

    ResenaInvalida error = assertThrows(
      ResenaInvalida.class,
      () -> servicioResena.crearResena(socio, 10L, 4, "Bien")
    );

    assertThat(
      error.getMessage(),
      equalTo("Solo podés reseñar turnos que la clínica marcó como atendidos")
    );
  }

  @Test
  public void turnoInexistenteDeberiaFallar() throws TurnoInvalido {
    when(servicioTurno.buscarTurnoDe(socio, 10L)).thenThrow(new TurnoInvalido("no está"));

    ResenaInvalida error = assertThrows(
      ResenaInvalida.class,
      () -> servicioResena.crearResena(socio, 10L, 4, "Bien")
    );

    assertThat(error.getMessage(), equalTo("No encontramos ese turno"));
  }

  @Test
  public void resenaRepetidaDeberiaFallar() {
    when(repositorioResena.existePorTurno(10L)).thenReturn(true);

    ResenaInvalida error = assertThrows(
      ResenaInvalida.class,
      () -> servicioResena.crearResena(socio, 10L, 4, "Bien")
    );

    assertThat(error.getMessage(), equalTo("Ya dejaste una reseña para este turno"));
    verify(repositorioResena, never()).guardar(any());
  }

  @Test
  public void socioYaResenoDeberiaConsultarElRepositorio() {
    when(repositorioResena.existePorTurno(10L)).thenReturn(true);

    assertThat(servicioResena.socioYaReseno(10L), equalTo(true));
  }

  @Test
  public void resumenSinResenasDeberiaTenerPromedioCero() {
    when(repositorioResena.listarPorVeterinaria(1L)).thenReturn(List.of());

    ResumenResenas resumen = servicioResena.resumenDeVeterinaria(1L);

    assertThat(resumen.getCantidad(), equalTo(0L));
    assertThat(resumen.getPromedio(), equalTo(0.0));
    assertThat(resumen.getUltimas(), hasSize(0));
  }

  @Test
  public void resumenConVariasResenasDeberiaPromediarYQuedarseConTres() {
    when(repositorioResena.listarPorVeterinaria(1L))
      .thenReturn(List.of(resena(5), resena(5), resena(4), resena(4)));

    ResumenResenas resumen = servicioResena.resumenDeVeterinaria(1L);

    assertThat(resumen.getCantidad(), equalTo(4L));
    assertThat(resumen.getPromedio(), equalTo(4.5));
    assertThat(resumen.getUltimas(), hasSize(3));
  }

  @Test
  public void resumenesYListadosDeberianArmarsePorCadaVeterinaria() {
    Veterinaria otra = new Veterinaria();
    otra.setId(2L);
    when(repositorioResena.listarPorVeterinaria(1L)).thenReturn(List.of(resena(5)));
    when(repositorioResena.listarPorVeterinaria(2L)).thenReturn(List.of());

    assertThat(
      servicioResena.resumenesPorVeterinarias(List.of(turno.getVeterinaria(), otra)).size(),
      equalTo(2)
    );
    assertThat(servicioResena.listarPorVeterinaria(1L), hasSize(1));
    assertThat(
      servicioResena.listarPorVeterinarias(List.of(turno.getVeterinaria(), otra)).size(),
      equalTo(2)
    );
  }

  private Resena resena(int puntuacion) {
    Resena resena = new Resena();
    resena.setPuntuacion(puntuacion);
    return resena;
  }
}
