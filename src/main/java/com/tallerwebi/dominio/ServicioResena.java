package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import java.util.List;
import java.util.Map;

public interface ServicioResena {
  void crearResena(Socio socio, Long turnoId, int puntuacion, String comentario)
    throws ResenaInvalida;

  boolean socioYaReseno(Long turnoId);

  ResumenResenas resumenDeVeterinaria(Long veterinariaId);

  Map<Long, ResumenResenas> resumenesPorVeterinarias(List<Veterinaria> lista);
}
