package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class RecetaTest {

  @Test
  public void unaRecetaNuevaDeberiaQuedarPendienteYSinFechaDeCompra() {
    Receta receta = new Receta(new Turno(), "Amoxicilina", "Cada 12 horas");

    assertThat(receta.estaPendiente(), is(true));
    assertThat(receta.getFechaCompra(), is(nullValue()));
  }

  @Test
  public void marcarLaRecetaComoCompradaDeberiaGuardarLaFecha() {
    Receta receta = new Receta(new Turno(), "Amoxicilina", "Cada 12 horas");
    LocalDate fecha = LocalDate.of(2026, 11, 10);

    receta.marcarComprada(fecha);

    assertThat(receta.estaPendiente(), is(false));
    assertThat(receta.getEstado(), is(EstadoReceta.COMPRADA));
    assertThat(receta.getFechaCompra(), is(fecha));
  }
}
