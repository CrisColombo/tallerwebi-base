package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface RepositorioTurno {
  void guardar(Turno turno);
  Turno buscarPorId(Long id);
  List<Turno> listarPorSocio(Long socioId);
  List<Turno> listarPorVeterinaria(Long veterinariaId);
  List<Turno> listarPorMascota(Long mascotaId);
  List<Turno> listarConfirmadosSinRecordatorio(LocalDate desde, LocalDate hasta);
  List<LocalTime> horasOcupadas(Long veterinariaId, LocalDate fecha);
}
