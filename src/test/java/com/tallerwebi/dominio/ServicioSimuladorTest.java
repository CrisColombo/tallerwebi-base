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

  private Turno[] consultasEnNoviembre(int cantidad) {
    Turno[] consultas = new Turno[cantidad];
    for (int i = 0; i < cantidad; i++) {
      consultas[i] = dadaUnaAtencion(LocalDate.of(2026, 11, i + 1), consulta, 8000.0);
    }
    return consultas;
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

  @Test
  public void deberiaCobrarSoloElCopagoSiLaAtencionEstaCubierta() throws Exception {
    basico.setCopago(2000.0);
    dadaUnaMascotaPropiaConAtenciones(dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 8000.0));

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getAtenciones().get(0).pagoCon(basico.getId()), equalTo(2000.0));
    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(2000.0));
    assertThat(simulacion.getPlanActual().getTotal(), equalTo(240000.0 + 2000.0));
  }

  @Test
  public void noDeberiaCobrarMasQueElPrecioParticularAunqueElCopagoSeaMayor() throws Exception {
    basico.setCopago(2000.0);
    dadaUnaMascotaPropiaConAtenciones(dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 1500.0));

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getAtenciones().get(0).pagoCon(basico.getId()), equalTo(1500.0));
  }

  @Test
  public void deberiaCobrarPrecioCompletoLasAtencionesQueSuperanElTopeMensual() throws Exception {
    basico.setTopeMensual(2);
    basico.setCopago(2000.0);
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 2), consulta, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 3), consulta, 8000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    // 2000 + 2000 (dentro del tope) + 8000 (la tercera ya no está cubierta)
    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(12000.0));
  }

  @Test
  public void deberiaReiniciarElTopeCadaMes() throws Exception {
    basico.setTopeMensual(1);
    basico.setCopago(2000.0);
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 9, 1), consulta, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 10, 1), consulta, 8000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(4000.0));
  }

  @Test
  public void noDeberiaGastarElTopeConAtencionesQueElPlanNoCubre() throws Exception {
    basico.setTopeMensual(1);
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 9, 1), cirugia, 400000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 2), consulta, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 3), consulta, 8000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    // cirugía: 400000 (no cubierta); 1ª consulta: 0 (usa el único cupo); 2ª consulta: 8000
    assertThat(simulacion.getPlanActual().getNoCubierto(), equalTo(408000.0));
  }

  @Test
  public void noDeberiaAplicarTopeNiCopagoAUnPlanQueNoLosTiene() throws Exception {
    dadaUnaMascotaPropiaConAtenciones(
      dadaUnaAtencion(LocalDate.of(2026, 9, 1), cirugia, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 2), cirugia, 8000.0),
      dadaUnaAtencion(LocalDate.of(2026, 9, 3), cirugia, 8000.0)
    );

    SimulacionPlan simulacion = servicioSimulador.simular(socio, MASCOTA_ID, 12);

    assertThat(simulacion.getResultados().get(1).getPlan(), is(premium));
    assertThat(simulacion.getResultados().get(1).getNoCubierto(), equalTo(0.0));
  }

  @Test
  public void deberiaConvenirElBasicoHastaSeisAtencionesEnElMesYPremiumDesdeLaSeptima()
    throws Exception {
    basico.setTopeMensual(4);
    basico.setCopago(2000.0);

    // Con 1 solo mes: cuota Básico 20000, cuota Premium 50000.
    dadaUnaMascotaPropiaConAtenciones(consultasEnNoviembre(6));
    SimulacionPlan conSeis = servicioSimulador.simular(socio, MASCOTA_ID, 1);
    // 20000 + 4 x 2000 + 2 x 8000 = 44000
    assertThat(conSeis.getPlanActual().getTotal(), equalTo(44000.0));
    assertThat(conSeis.getMasConveniente().getPlan(), is(basico));

    dadaUnaMascotaPropiaConAtenciones(consultasEnNoviembre(7));
    SimulacionPlan conSiete = servicioSimulador.simular(socio, MASCOTA_ID, 1);
    // 20000 + 4 x 2000 + 3 x 8000 = 52000, contra 50000 del Premium
    assertThat(conSiete.getPlanActual().getTotal(), equalTo(52000.0));
    assertThat(conSiete.getMasConveniente().getPlan(), is(premium));
  }
}
