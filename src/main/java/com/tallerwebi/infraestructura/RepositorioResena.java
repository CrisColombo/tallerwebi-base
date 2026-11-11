package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Resena;
import java.util.List;

public interface RepositorioResena {
  void guardar(Resena resena);

  boolean existePorTurno(Long turnoId);

  List<Resena> listarPorVeterinaria(Long veterinariaId);
}
