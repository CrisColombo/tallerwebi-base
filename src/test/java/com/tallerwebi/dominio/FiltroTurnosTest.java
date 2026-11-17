package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FiltroTurnosTest {

  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
  private static final LocalDateTime AHORA = HOY.atTime(LocalTime.NOON);
  private static final Predicate<Turno> NINGUNO_RESENADO = t -> false;

  private FiltroTurnos filtro;
  private Mascota michi;
  private Mascota firulais;
  private Veterinaria sanJusto;
  private Veterinaria animalVet;
  private Servicio consulta;
  private Servicio vacunacion;

  @BeforeEach
  public void init() {
    filtro = new FiltroTurnos();
    michi = dadaUnaMascota(2L, "Michi", Especie.GATO);
    firulais = dadaUnaMascota(1L, "Firulais", Especie.PERRO);
    sanJusto = dadaUnaVeterinaria(1L, "Clínica Veterinaria San Justo");
    animalVet = dadaUnaVeterinaria(8L, "Animal Vet");
    consulta = dadoUnServicio(1L, "Consulta clínica");
    vacunacion = dadoUnServicio(2L, "Vacunación");
  }

  @Test
  public void porDefectoDeberiaMostrarLosProximosDelMasCercanoAlMasLejano() {
    Turno lejano = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(9), EstadoTurno.CONFIRMADO);
    Turno cercano = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(2), EstadoTurno.CONFIRMADO);
    Turno pasado = dadoUnTurno(michi, sanJusto, consulta, HOY.minusDays(2), EstadoTurno.CONFIRMADO);

    List<Turno> resultado = filtro.aplicar(
      List.of(lejano, cercano, pasado),
      AHORA,
      NINGUNO_RESENADO
    );

    assertThat(resultado, contains(cercano, lejano));
  }

  @Test
  public void laPestanaAtendidosDeberiaOrdenarDelMasNuevoAlMasViejo() {
    Turno viejo = dadoUnTurno(michi, sanJusto, consulta, HOY.minusDays(40), EstadoTurno.FINALIZADO);
    Turno nuevo = dadoUnTurno(michi, sanJusto, consulta, HOY.minusDays(3), EstadoTurno.FINALIZADO);
    Turno cancelado = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(5),
      EstadoTurno.CANCELADO
    );
    filtro.setVista(FiltroTurnos.ATENDIDOS);

    assertThat(
      filtro.aplicar(List.of(viejo, nuevo, cancelado), AHORA, NINGUNO_RESENADO),
      contains(nuevo, viejo)
    );
  }

  @Test
  public void deberiaFiltrarPorMascotaEspecieVeterinariaYServicio() {
    Turno buscado = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(1), EstadoTurno.CONFIRMADO);
    Turno otraMascota = dadoUnTurno(
      firulais,
      sanJusto,
      consulta,
      HOY.plusDays(1),
      EstadoTurno.CONFIRMADO
    );
    Turno otraVeterinaria = dadoUnTurno(
      michi,
      animalVet,
      consulta,
      HOY.plusDays(2),
      EstadoTurno.CONFIRMADO
    );
    Turno otroServicio = dadoUnTurno(
      michi,
      sanJusto,
      vacunacion,
      HOY.plusDays(3),
      EstadoTurno.CONFIRMADO
    );
    filtro.setMascota(List.of(2L));
    filtro.setEspecie(Especie.GATO);
    filtro.setVeterinaria(1L);
    filtro.setServicio(1L);

    List<Turno> resultado = filtro.aplicar(
      List.of(buscado, otraMascota, otraVeterinaria, otroServicio),
      AHORA,
      NINGUNO_RESENADO
    );

    assertThat(resultado, contains(buscado));
    assertThat(filtro.isActivo(), is(true));
  }

  @Test
  public void deberiaFiltrarPorRangoDeFechas() {
    Turno antes = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(1), EstadoTurno.CONFIRMADO);
    Turno dentro = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(5), EstadoTurno.CONFIRMADO);
    Turno despues = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.plusDays(30),
      EstadoTurno.CONFIRMADO
    );
    filtro.setDesde(HOY.plusDays(3));
    filtro.setHasta(HOY.plusDays(10));

    assertThat(
      filtro.aplicar(List.of(antes, dentro, despues), AHORA, NINGUNO_RESENADO),
      contains(dentro)
    );
  }

  @Test
  public void soloSinResenaDeberiaDejarLosAtendidosQueNoTienenResena() {
    Turno resenado = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(9),
      EstadoTurno.FINALIZADO
    );
    Turno sinResena = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(4),
      EstadoTurno.FINALIZADO
    );
    filtro.setVista(FiltroTurnos.TODOS);
    filtro.setSinResena(true);

    List<Turno> resultado = filtro.aplicar(List.of(resenado, sinResena), AHORA, t -> t == resenado);

    assertThat(resultado, contains(sinResena));
  }

  @Test
  public void laBusquedaDeberiaMirarMascotaVeterinariaServicioYObservaciones() {
    Turno conOtitis = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(4),
      EstadoTurno.FINALIZADO
    );
    conOtitis.finalizar("Otitis leve en el oído derecho");
    Turno otro = dadoUnTurno(
      firulais,
      animalVet,
      vacunacion,
      HOY.minusDays(8),
      EstadoTurno.FINALIZADO
    );
    filtro.setVista(FiltroTurnos.ATENDIDOS);
    filtro.setTexto(" OTITIS ");

    assertThat(
      filtro.aplicar(List.of(conOtitis, otro), AHORA, NINGUNO_RESENADO),
      contains(conOtitis)
    );
    filtro.setTexto("animal vet");
    assertThat(filtro.aplicar(List.of(conOtitis, otro), AHORA, NINGUNO_RESENADO), contains(otro));
  }

  @Test
  public void losConteosDeberianRespetarLosFiltrosEnCadaPestana() {
    Turno proximo = dadoUnTurno(michi, sanJusto, consulta, HOY.plusDays(1), EstadoTurno.CONFIRMADO);
    Turno atendido = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(3),
      EstadoTurno.FINALIZADO
    );
    Turno cancelado = dadoUnTurno(
      michi,
      sanJusto,
      consulta,
      HOY.minusDays(6),
      EstadoTurno.CANCELADO
    );
    Turno deFirulais = dadoUnTurno(
      firulais,
      sanJusto,
      consulta,
      HOY.plusDays(2),
      EstadoTurno.CONFIRMADO
    );
    filtro.setMascota(List.of(2L));

    Map<String, Long> conteos = filtro.conteos(
      List.of(proximo, atendido, cancelado, deFirulais),
      AHORA,
      NINGUNO_RESENADO
    );

    assertThat(conteos.get(FiltroTurnos.PROXIMOS), is(1L));
    assertThat(conteos.get(FiltroTurnos.ATENDIDOS), is(1L));
    assertThat(conteos.get(FiltroTurnos.CANCELADOS), is(1L));
    assertThat(conteos.get(FiltroTurnos.TODOS), is(3L));
  }

  @Test
  public void unaVistaDesconocidaDeberiaVolverAProximosYSinFiltrosNoEstaActivo() {
    filtro.setVista("cualquiera");
    filtro.setMascota(null);
    filtro.setTexto("  ");

    assertThat(filtro.getVista(), is(FiltroTurnos.PROXIMOS));
    assertThat(filtro.isActivo(), is(false));
  }

  private Turno dadoUnTurno(
    Mascota mascota,
    Veterinaria veterinaria,
    Servicio servicio,
    LocalDate fecha,
    EstadoTurno estado
  ) {
    Turno turno = new Turno();
    turno.setMascota(mascota);
    turno.setVeterinaria(veterinaria);
    turno.setServicio(servicio);
    turno.setFecha(fecha);
    turno.setHora(LocalTime.of(10, 0));
    turno.setEstado(estado);
    return turno;
  }

  private Mascota dadaUnaMascota(Long id, String nombre, Especie especie) {
    Mascota mascota = new Mascota();
    mascota.setId(id);
    mascota.setNombre(nombre);
    mascota.setEspecie(especie);
    return mascota;
  }

  private Veterinaria dadaUnaVeterinaria(Long id, String nombre) {
    Veterinaria veterinaria = new Veterinaria();
    veterinaria.setId(id);
    veterinaria.setNombre(nombre);
    return veterinaria;
  }

  private Servicio dadoUnServicio(Long id, String nombre) {
    Servicio servicio = new Servicio();
    servicio.setId(id);
    servicio.setNombre(nombre);
    return servicio;
  }
}
