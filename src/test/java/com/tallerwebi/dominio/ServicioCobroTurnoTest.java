package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioCobroTurnoTest {

  private static final Long TURNO_ID = 10L;

  private ServicioTurno servicioTurnoMock;
  private RepositorioTurno repositorioTurnoMock;
  private RepositorioServicio repositorioServicioMock;
  private ServicioCobroTurno servicioCobro;

  private Veterinaria veterinaria;
  private Plan basico;
  private Servicio consulta;
  private Servicio radiografia;
  private Turno turno;

  @BeforeEach
  public void init() throws Exception {
    servicioTurnoMock = mock(ServicioTurno.class);
    repositorioTurnoMock = mock(RepositorioTurno.class);
    repositorioServicioMock = mock(RepositorioServicio.class);
    servicioCobro =
      new ServicioCobroTurnoImpl(servicioTurnoMock, repositorioTurnoMock, repositorioServicioMock);

    veterinaria = mock(Veterinaria.class);
    basico = new Plan();
    basico.setNivel(1);
    basico.setTopeMensual(4);
    basico.setCopago(2000.0);
    consulta = dadoUnServicio(1L, "Consulta clínica", 1, 8000.0);
    radiografia = dadoUnServicio(2L, "Radiografía", 2, 15000.0);

    Mascota firulais = new Mascota();
    firulais.setPlan(basico);
    turno = new Turno();
    turno.setId(TURNO_ID);
    turno.setMascota(firulais);
    turno.setServicio(consulta);
    turno.setFecha(LocalDate.of(2026, 11, 12));

    when(servicioTurnoMock.buscarTurnoDeVeterinaria(veterinaria, TURNO_ID)).thenReturn(turno);
    when(repositorioTurnoMock.listarPorMascota(any())).thenReturn(List.of());
    when(repositorioServicioMock.buscarPorId(2L)).thenReturn(radiografia);
    when(veterinaria.ofrece(radiografia)).thenReturn(true);
  }

  @Test
  public void deberiaCalcularLoQuePagaElSocioSegunSuPlan() throws Exception {
    CobroTurno cobro = servicioCobro.calcularCobro(veterinaria, TURNO_ID, List.of(2L));

    assertThat(cobro.getLineas(), hasSize(2));
    assertThat(cobro.getLineas().get(0).getPagaElSocio(), equalTo(2000.0));
    assertThat(cobro.getLineas().get(1).getPagaElSocio(), equalTo(15000.0));
    assertThat(cobro.getTotalParticular(), equalTo(23000.0));
    assertThat(cobro.getTotalSocio(), equalTo(17000.0));
  }

  @Test
  public void deberiaCalcularSoloElServicioDelTurnoSiNoHayAdicionales() throws Exception {
    CobroTurno cobro = servicioCobro.calcularCobro(veterinaria, TURNO_ID, null);

    assertThat(cobro.getLineas(), hasSize(1));
    assertThat(cobro.getTotalSocio(), equalTo(2000.0));
  }

  @Test
  public void deberiaCobrarTodoSiLaMascotaNoTienePlan() throws Exception {
    turno.getMascota().setPlan(null);

    CobroTurno cobro = servicioCobro.calcularCobro(veterinaria, TURNO_ID, List.of(2L));

    assertThat(cobro.getTotalSocio(), equalTo(23000.0));
  }

  @Test
  public void deberiaDescontarElCupoDeLasAtencionesYaRealizadasEnElMes() throws Exception {
    basico.setTopeMensual(1);
    when(repositorioTurnoMock.listarPorMascota(any()))
      .thenReturn(List.of(dadoUnTurnoFinalizado(1L, LocalDate.of(2026, 11, 3))));

    CobroTurno cobro = servicioCobro.calcularCobro(veterinaria, TURNO_ID, null);

    assertThat(cobro.getTotalSocio(), equalTo(8000.0));
  }

  @Test
  public void noDeberiaContarLasAtencionesDeOtroMes() throws Exception {
    basico.setTopeMensual(1);
    when(repositorioTurnoMock.listarPorMascota(any()))
      .thenReturn(List.of(dadoUnTurnoFinalizado(1L, LocalDate.of(2026, 10, 20))));

    CobroTurno cobro = servicioCobro.calcularCobro(veterinaria, TURNO_ID, null);

    assertThat(cobro.getTotalSocio(), equalTo(2000.0));
  }

  @Test
  public void noDeberiaCalcularConUnServicioQueLaVeterinariaNoOfrece() {
    when(veterinaria.ofrece(radiografia)).thenReturn(false);

    assertThrows(
      TurnoInvalido.class,
      () -> servicioCobro.calcularCobro(veterinaria, TURNO_ID, List.of(2L))
    );
  }

  @Test
  public void noDeberiaCalcularConUnServicioSinPrecioCargado() {
    radiografia.setPrecioParticular(null);

    assertThrows(
      TurnoInvalido.class,
      () -> servicioCobro.calcularCobro(veterinaria, TURNO_ID, List.of(2L))
    );
  }

  @Test
  public void deberiaFinalizarConElPrecioDeCatalogoYLosAdicionales() throws Exception {
    servicioCobro.finalizarConAdicionales(veterinaria, TURNO_ID, " Todo bien ", List.of(2L));

    assertThat(turno.getEstado(), is(EstadoTurno.FINALIZADO));
    assertThat(turno.getObservacionesProfesional(), equalTo("Todo bien"));
    assertThat(turno.getPrecioParticular(), equalTo(8000.0));
    assertThat(turno.getAdicionales(), hasSize(1));
    assertThat(turno.getTotalParticular(), equalTo(23000.0));
  }

  @Test
  public void deberiaFinalizarSinAdicionales() throws Exception {
    servicioCobro.finalizarConAdicionales(veterinaria, TURNO_ID, null, null);

    assertThat(turno.getEstado(), is(EstadoTurno.FINALIZADO));
    assertThat(turno.getAdicionales(), hasSize(0));
    assertThat(turno.getTotalParticular(), equalTo(8000.0));
  }

  @Test
  public void noDeberiaFinalizarUnTurnoQueNoEstaConfirmado() {
    turno.setEstado(EstadoTurno.CANCELADO);

    assertThrows(
      TurnoInvalido.class,
      () -> servicioCobro.finalizarConAdicionales(veterinaria, TURNO_ID, null, List.of(2L))
    );
  }

  @Test
  public void noDeberiaDejarElTurnoAMedioFinalizarSiUnAdicionalEsInvalido() {
    when(veterinaria.ofrece(radiografia)).thenReturn(false);

    assertThrows(
      TurnoInvalido.class,
      () -> servicioCobro.finalizarConAdicionales(veterinaria, TURNO_ID, "Obs", List.of(2L))
    );

    assertThat(turno.getEstado(), is(EstadoTurno.CONFIRMADO));
    assertThat(turno.getAdicionales(), hasSize(0));
  }

  private Turno dadoUnTurnoFinalizado(Long id, LocalDate fecha) {
    Turno previo = new Turno();
    previo.setId(id);
    previo.setFecha(fecha);
    previo.setServicio(consulta);
    previo.setEstado(EstadoTurno.FINALIZADO);
    return previo;
  }

  private Servicio dadoUnServicio(Long id, String nombre, Integer nivel, Double precio) {
    Servicio servicio = new Servicio();
    servicio.setId(id);
    servicio.setNombre(nombre);
    servicio.setNivelRequerido(nivel);
    servicio.setPrecioParticular(precio);
    return servicio;
  }
}
