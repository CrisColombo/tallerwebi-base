package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioReceta {
  void guardar(Receta receta);
  Receta buscarPorId(Long id);
  List<Receta> listarPorMascota(Long mascotaId);
}
