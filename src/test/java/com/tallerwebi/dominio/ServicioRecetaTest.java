package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioRecetaTest {

  private RepositorioReceta repositorioRecetaMock;
  private ServicioReceta servicioReceta;
  private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
  private Socio socio;
  private Mascota firulais;

  @BeforeEach
  public void init() {
    repositorioRecetaMock = mock(RepositorioReceta.class);
    Clock reloj = Clock.fixed(
      HOY.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
      ZoneId.systemDefault()
    );
    servicioReceta = new ServicioRecetaImpl(repositorioRecetaMock, reloj);
    socio = new Socio();
    firulais = new Mascota();
    firulais.setId(7L);
    socio.agregarMascota(firulais);
  }

  @Test
  public void registrarDeberiaGuardarUnaRecetaPorCadaMedicamentoCargado() {
    Turno turno = new Turno();
    List<DatosReceta> datos = List.of(
      new DatosReceta("Amoxicilina", "Cada 12 horas"),
      new DatosReceta("Meloxicam", "Cada 24 horas")
    );

    servicioReceta.registrar(turno, datos);

    verify(repositorioRecetaMock, times(2)).guardar(any(Receta.class));
  }

  @Test
  public void registrarDeberiaGuardarLaRecetaPendienteYAsociadaAlTurno() {
    Turno turno = new Turno();
    ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);

    servicioReceta.registrar(turno, List.of(new DatosReceta("  Amoxicilina ", " Cada 12 horas ")));

    verify(repositorioRecetaMock).guardar(captor.capture());
    Receta guardada = captor.getValue();
    org.hamcrest.MatcherAssert.assertThat(
      guardada.getMedicamento(),
      org.hamcrest.Matchers.is("Amoxicilina")
    );
    org.hamcrest.MatcherAssert.assertThat(
      guardada.getIndicaciones(),
      org.hamcrest.Matchers.is("Cada 12 horas")
    );
    org.hamcrest.MatcherAssert.assertThat(guardada.getTurno(), org.hamcrest.Matchers.is(turno));
    org.hamcrest.MatcherAssert.assertThat(guardada.estaPendiente(), org.hamcrest.Matchers.is(true));
  }

  @Test
  public void registrarDeberiaIgnorarLasFilasSinMedicamento() {
    List<DatosReceta> datos = Arrays.asList(
      new DatosReceta("", "Cada 12 horas"),
      new DatosReceta(null, null),
      new DatosReceta("   ", "")
    );

    servicioReceta.registrar(new Turno(), datos);

    verify(repositorioRecetaMock, never()).guardar(any(Receta.class));
  }

  @Test
  public void registrarSinListaNoDeberiaHacerNada() {
    servicioReceta.registrar(new Turno(), null);

    verifyNoInteractions(repositorioRecetaMock);
  }

  @Test
  public void recetasDeDeberiaAgruparLasRecetasPorConsulta() throws TurnoInvalido {
    Turno reciente = dadoUnTurnoDe(1L, firulais);
    Turno anterior = dadoUnTurnoDe(2L, firulais);
    when(repositorioRecetaMock.listarPorMascota(7L))
      .thenReturn(
        List.of(
          new Receta(reciente, "Amoxicilina", ""),
          new Receta(reciente, "Meloxicam", ""),
          new Receta(anterior, "Antiparasitario", "")
        )
      );

    List<RecetasDeConsulta> consultas = servicioReceta.recetasDe(socio, 7L);

    assertThat(consultas, hasSize(2));
    assertThat(consultas.get(0).getTurno(), is(reciente));
    assertThat(consultas.get(0).getRecetas(), hasSize(2));
    assertThat(consultas.get(1).getRecetas(), hasSize(1));
  }

  @Test
  public void recetasDeNoDeberiaPermitirVerLasDeUnaMascotaAjena() {
    assertThrows(TurnoInvalido.class, () -> servicioReceta.recetasDe(socio, 99L));
  }

  @Test
  public void marcarCompradaDeberiaDejarLaRecetaCompradaConLaFechaDeHoy() throws TurnoInvalido {
    Receta receta = new Receta(dadoUnTurnoDe(1L, firulais), "Amoxicilina", "");
    when(repositorioRecetaMock.buscarPorId(3L)).thenReturn(receta);

    servicioReceta.marcarComprada(socio, 3L);

    assertThat(receta.getEstado(), is(EstadoReceta.COMPRADA));
    assertThat(receta.getFechaCompra(), is(HOY));
  }

  @Test
  public void marcarCompradaNoDeberiaPermitirUnaRecetaDeOtroSocio() {
    Mascota ajena = new Mascota();
    ajena.setId(50L);
    Receta receta = new Receta(dadoUnTurnoDe(1L, ajena), "Amoxicilina", "");
    when(repositorioRecetaMock.buscarPorId(3L)).thenReturn(receta);

    assertThrows(TurnoInvalido.class, () -> servicioReceta.marcarComprada(socio, 3L));
  }

  @Test
  public void marcarCompradaNoDeberiaPermitirUnaRecetaQueNoExiste() {
    assertThrows(TurnoInvalido.class, () -> servicioReceta.marcarComprada(socio, 3L));
  }

  @Test
  public void marcarCompradaNoDeberiaPermitirUnaRecetaYaComprada() {
    Receta receta = new Receta(dadoUnTurnoDe(1L, firulais), "Amoxicilina", "");
    receta.marcarComprada(HOY.minusDays(1));
    when(repositorioRecetaMock.buscarPorId(3L)).thenReturn(receta);

    assertThrows(TurnoInvalido.class, () -> servicioReceta.marcarComprada(socio, 3L));
  }

  @Test
  public void medicacionAnteriorDeberiaMostrarLasRecetasDeTodasLasVeterinarias()
    throws TurnoInvalido {
    Veterinaria propia = dadaUnaVeterinaria(7L);
    Turno turnoActual = dadoUnTurnoDe(1L, firulais);
    turnoActual.setVeterinaria(propia);
    Turno deOtraVeterinaria = dadoUnTurnoDe(2L, firulais);
    deOtraVeterinaria.setVeterinaria(dadaUnaVeterinaria(8L));
    when(repositorioRecetaMock.listarPorMascota(7L))
      .thenReturn(List.of(new Receta(deOtraVeterinaria, "Amoxicilina", "")));

    List<RecetasDeConsulta> consultas = servicioReceta.medicacionAnterior(propia, turnoActual);

    assertThat(consultas, hasSize(1));
    assertThat(consultas.get(0).getTurno(), is(deOtraVeterinaria));
  }

  @Test
  public void medicacionAnteriorNoDeberiaPermitirUnTurnoDeOtraVeterinaria() {
    Turno turno = dadoUnTurnoDe(1L, firulais);
    turno.setVeterinaria(dadaUnaVeterinaria(8L));

    assertThrows(
      TurnoInvalido.class,
      () -> servicioReceta.medicacionAnterior(dadaUnaVeterinaria(7L), turno)
    );
  }

  @Test
  public void medicacionAnteriorNoDeberiaPermitirUnTurnoCancelado() {
    Veterinaria propia = dadaUnaVeterinaria(7L);
    Turno turno = dadoUnTurnoDe(1L, firulais);
    turno.setVeterinaria(propia);
    turno.cancelar();

    assertThrows(TurnoInvalido.class, () -> servicioReceta.medicacionAnterior(propia, turno));
  }

  @Test
  public void medicacionAnteriorNoDeberiaPermitirUnTurnoSinVeterinaria() {
    Turno turno = dadoUnTurnoDe(1L, firulais);

    assertThrows(
      TurnoInvalido.class,
      () -> servicioReceta.medicacionAnterior(dadaUnaVeterinaria(7L), turno)
    );
  }

  private Veterinaria dadaUnaVeterinaria(Long id) {
    Veterinaria veterinaria = new Veterinaria();
    veterinaria.setId(id);
    return veterinaria;
  }

  private Turno dadoUnTurnoDe(Long id, Mascota mascota) {
    Turno turno = new Turno();
    turno.setId(id);
    turno.setMascota(mascota);
    return turno;
  }
}
