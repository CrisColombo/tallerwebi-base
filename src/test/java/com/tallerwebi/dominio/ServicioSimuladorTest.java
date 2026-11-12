package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioSimuladorTest {

  // "Hoy" es el 12/11/2026.
  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
  private static final Long MASCOTA_ID = 1L;

  private ServicioMascota servicioMascotaMock;
  private ServicioTurno servicioTurnoMock;
  private RepositorioPlan repositorioPlanMock;
  private ServicioSimulador servicioSimulador;

  private Socio socio;
  private Plan basico;
  private Plan premium;
  private Servicio consulta;
  private Servicio cirugia;
  private Mascota firulais;

  @BeforeEach
  public void init() {
    servicioMascotaMock = mock(ServicioMascota.class);
    servicioTurnoMock = mock(ServicioTurno.class);
    repositorioPlanMock = mock(RepositorioPlan.class);
    Clock reloj = Clock.fixed(
      LocalDateTime.of(HOY, LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
      ZoneId.systemDefault()
    );
    servicioSimulador =
      new ServicioSimuladorImpl(servicioMascotaMock, servicioTurnoMock, repositorioPlanMock, reloj);

    basico = dadoUnPlan(1L, "Plan Básico", 20000.0, 1);
    premium = dadoUnPlan(2L, "Plan Premium", 50000.0, 2);
    consulta = dadoUnServicio(1L, "Consulta clínica", 1);
    cirugia = dadoUnServicio(2L, "Cirugía", 2);
    socio = new Socio();
    firulais = new Mascota();
    firulais.setPlan(basico);
    when(repositorioPlanMock.listar()).thenReturn(List.of(basico, premium));
  }

  @Test
  public void deberiaConvenirPremiumSiLaMascotaUsaServiciosQueElBasicoNoCubre() throws Exception {
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 10, 1), cirugia, 400000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    ResultadoPlan resultadoBasico = simulacion.getPlanActual();
    assertThat(resultadoBasico.getTotal(), equalTo(240000.0 + 400000.0));
    assertThat(simulacion.getMasConveniente().getPlan(), is(premium));
    assertThat(simulacion.getMasConveniente().getTotal(), equalTo(600000.0));
    assertThat(simulacion.getAhorro(), equalTo(40000.0));
    assertThat(simulacion.isPlanActualEsElMejor(), is(false));
  }

  @Test
  public void deberiaMantenerElPlanActualSiSoloUsaServiciosQueCubre() throws Exception {
    dadaUnaMascotaPropiaConAtenciones(dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 8000.0));

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(0.0));
    assertThat(simulacion.getMasConveniente().getPlan(), is(basico));
    assertThat(simulacion.getAhorro(), equalTo(0.0));
    assertThat(simulacion.isPlanActualEsElMejor(), is(true));
  }

  @Test
  public void deberiaIndicarQuePlanesCubrenCadaAtencion() throws Exception {
    dadaUnaMascotaPropiaConAtenciones(dadaUnaAtencion(LocalDate.of(2026, 9, 1), cirugia, 1000.0));

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    ItemAtencion item = simulacion.getAtenciones().get(0);
    assertThat(item.cubiertoPor(basico.getId()), is(false));
    assertThat(item.cubiertoPor(premium.getId()), is(true));
  }

  @Test
  public void deberiaIgnorarAtencionesSinPrecioNoFinalizadasOFueraDelPeriodo() throws Exception {
    Turno sinPrecio = dadaUnaAtencion(LocalDate.of(2026, 9, 1), cirugia, null);
    Turno confirmado = dadaUnaAtencion(LocalDate.of(2026, 9, 2), cirugia, 5000.0);
    confirmado.setEstado(EstadoTurno.CONFIRMADO);
    Turno viejo = dadaUnaAtencion(LocalDate.of(2025, 1, 1), cirugia, 5000.0);
    Turno valido = dadaUnaAtencion(LocalDate.of(2026, 9, 3), cirugia, 7000.0);
    dadaUnaMascotaPropiaConAtenciones(sinPrecio, confirmado, viejo, valido);

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getAtenciones(), hasSize(1));
    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(7000.0));
  }

  @Test
  public void deberiaIndicarQueNoHayAtencionesSiNoHayConsultasFinalizadas() throws Exception {
    dadaUnaMascotaPropiaConAtenciones();

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.isSinAtenciones(), is(true));
    assertThat(simulacion.getPlanActual().getTotal(), equalTo(240000.0));
  }

  @Test
  public void noDeberiaSimularUnaMascotaQueNoEsDelSocio() throws Exception {
    when(servicioMascotaMock.buscarPorIdParaSocio(socio, MASCOTA_ID)).thenReturn(null);

    assertThat(servicioSimulador.simular(socio, MASCOTA_ID, 12), is(nullValue()));
  }

  @Test
  public void deberiaCalcularConElPeriodoPedido() throws Exception {
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 3, 1), cirugia, 9000.0),
      dadaUnaAtencion(LocalDate.of(2026, 10, 1), cirugia, 1000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 3);

    assertThat(simulacion.getAtenciones(), hasSize(1));
    assertThat(simulacion.getPlanActual().getCuotaTotal(), equalTo(60000.0));
    assertThat(simulacion.getMeses(), equalTo(3));
  }

  private void dadaUnaMascotaPropiaConAtenciones(Turno... atenciones) throws Exception {
    when(servicioMascotaMock.buscarPorIdParaSocio(socio, MASCOTA_ID)).thenReturn(firulais);
    when(servicioTurnoMock.historialDe(socio, MASCOTA_ID))
      .thenReturn(new HistorialTurnos(List.of(), List.of(atenciones)));
  }

  private Turno dadaUnaAtencion(LocalDate fecha, Servicio servicio, Double precio) {
    Turno turno = new Turno();
    turno.setFecha(fecha);
    turno.setServicio(servicio);
    turno.setPrecioParticular(precio);
    turno.setEstado(EstadoTurno.FINALIZADO);
    return turno;
  }

  private Plan dadoUnPlan(Long id, String nombre, Double precio, Integer nivel) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setNombre(nombre);
    plan.setPrecio(precio);
    plan.setNivel(nivel);
    return plan;
  }

  private Servicio dadoUnServicio(Long id, String nombre, Integer nivelRequerido) {
    Servicio servicio = new Servicio();
    servicio.setId(id);
    servicio.setNombre(nombre);
    servicio.setNivelRequerido(nivelRequerido);
    return servicio;
  }
}
