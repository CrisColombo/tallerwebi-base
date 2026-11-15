package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ServicioPlanTest {

  @Test
  public void listarDeberiaDevolverLosPlanesDelRepositorio() {
    RepositorioPlan repositorioPlan = mock(RepositorioPlan.class);
    Plan basico = new Plan();
    when(repositorioPlan.listar()).thenReturn(List.of(basico));

    List<Plan> planes = new ServicioPlanImpl(repositorioPlan).listar();

    assertThat(planes, contains(basico));
  }
}
