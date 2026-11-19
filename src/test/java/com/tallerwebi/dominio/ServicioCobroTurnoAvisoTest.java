package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioCobroTurnoAvisoTest {

  private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

  private RepositorioTurno repositorioTurno;
  private RepositorioServicio repositorioServicio;
  private ServicioCobroTurnoImpl servicio;
  private Mascota mascota;
  private Servicio consulta;

  @BeforeEach
  public void init() {
    repositorioTurno = mock(RepositorioTurno.class);
    repositorioServicio = mock(RepositorioServicio.class);
    servicio =
      new ServicioCobroTurnoImpl(mock(ServicioTurno.class), repositorioTurno, repositorioServicio);

    Plan basico = new Plan();
    basico.setNombre("Básico");
    basico.setNivel(1);
    basico.setTopeMensual(4);
    basico.setCopago(2000.0);

    mascota = new Mascota();
    mascota.setId(1L);
    mascota.setPlan(basico);

    consulta = new Servicio();
    consulta.setNombre("Consulta clínica");
    consulta.setNivelRequerido(1);
    consulta.setPrecioParticular(8000.0);
    when(repositorioServicio.buscarPorId(10L)).thenReturn(consulta);
  }

  @Test
  public void deberiaAvisarCuantasAtencionesQuedanYElCopago() {
    when(repositorioTurno.listarPorMascota(1L)).thenReturn(atencionesFinalizadas(1));

    AvisoCobro aviso = servicio.avisoPara(mascota, 10L, FECHA);

    assertThat(aviso.getMensaje(), containsString("Te quedan 3 atenciones"));
    assertThat(aviso.getMensaje(), containsString("copago de $2.000"));
    assertThat(aviso.isAdvertencia(), is(false));
  }

  @Test
  public void deberiaAvisarQueSePagaElPrecioParticularCuandoSeAgotoElTope() {
    when(repositorioTurno.listarPorMascota(1L)).thenReturn(atencionesFinalizadas(4));

    AvisoCobro aviso = servicio.avisoPara(mascota, 10L, FECHA);

    assertThat(aviso.getMensaje(), containsString("Ya usaste las 4 atenciones"));
    assertThat(aviso.getMensaje(), containsString("$8.000"));
    assertThat(aviso.isAdvertencia(), is(true));
  }

  @Test
  public void deberiaAvisarQueElPlanNoCubreElServicio() {
    consulta.setNivelRequerido(2);
    when(repositorioTurno.listarPorMascota(1L)).thenReturn(new ArrayList<>());

    AvisoCobro aviso = servicio.avisoPara(mascota, 10L, FECHA);

    assertThat(aviso.getMensaje(), containsString("no cubre este servicio"));
    assertThat(aviso.isAdvertencia(), is(true));
  }

  @Test
  public void noDeberiaAvisarSiLaMascotaNoTienePlanOFaltaElServicio() {
    assertThat(servicio.avisoPara(mascota, null, FECHA), is(nullValue()));
    mascota.setPlan(null);
    assertThat(servicio.avisoPara(mascota, 10L, FECHA), is(nullValue()));
  }

  private List<Turno> atencionesFinalizadas(int cantidad) {
    List<Turno> turnos = new ArrayList<>();
    for (int i = 0; i < cantidad; i++) {
      Turno turno = new Turno();
      turno.setMascota(mascota);
      turno.setServicio(consulta);
      turno.setFecha(LocalDate.of(2026, 11, 2 + i));
      turno.setEstado(EstadoTurno.FINALIZADO);
      turnos.add(turno);
    }
    return turnos;
  }
}
