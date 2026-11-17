package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class MascotaTest {

  @Test
  public void laEdadDeberiaContarSoloLosAniosCumplidos() {
    Mascota mascota = new Mascota();
    mascota.setFechaNacimiento(LocalDate.of(2021, 8, 22));

    assertThat(mascota.edadEnAnios(LocalDate.of(2026, 8, 21)), is(4));
    assertThat(mascota.edadEnAnios(LocalDate.of(2026, 8, 22)), is(5));
  }

  @Test
  public void sinFechaDeNacimientoNoDeberiaTenerEdad() {
    Mascota mascota = new Mascota();

    assertThat(mascota.edadEnAnios(LocalDate.of(2026, 1, 1)), is(nullValue()));
    assertThat(mascota.getEdad(), is(nullValue()));
  }
}
