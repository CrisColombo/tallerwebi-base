package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import java.util.List;

public interface ServicioCobroTurno {
  /** Lo que cuesta el turno y lo que le toca pagar al socio según su plan. */
  CobroTurno calcularCobro(
    Veterinaria veterinaria,
    Long turnoId,
    List<Long> serviciosAdicionalesIds
  ) throws TurnoInvalido;

  /** Finaliza el turno con los servicios extra y los precios de catálogo. */
  void finalizarConAdicionales(
    Veterinaria veterinaria,
    Long turnoId,
    String observaciones,
    List<Long> serviciosAdicionalesIds
  ) throws TurnoInvalido;
}
