package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioSimulador {
  /** Devuelve null si la mascota no existe o no es del socio. */
  SimulacionPlan simular(Socio socio, Long mascotaId, int meses) throws TurnoInvalido;
}
