package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import com.tallerwebi.infraestructura.RepositorioResena;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioResenaImpl implements ServicioResena {

  private static final int PUNTUACION_MIN = 1;
  private static final int PUNTUACION_MAX = 5;
  private static final int MAX_ULTIMAS = 3;
  private static final double FACTOR_DECIMAL = 10.0;

  private final RepositorioResena repositorioResena;
  private final ServicioTurno servicioTurno;

  @Autowired
  public ServicioResenaImpl(RepositorioResena repositorioResena, ServicioTurno servicioTurno) {
    this.repositorioResena = repositorioResena;
    this.servicioTurno = servicioTurno;
  }

  @Override
  public void crearResena(Socio socio, Long turnoId, int puntuacion, String comentario)
    throws ResenaInvalida {
    exigirPuntuacion(puntuacion);
    exigirComentario(comentario);
    Turno turno = buscarTurnoDelSocio(socio, turnoId);
    exigirFinalizado(turno);
    if (repositorioResena.existePorTurno(turnoId)) {
      throw new ResenaInvalida("Ya dejaste una reseña para este turno");
    }
    repositorioResena.guardar(armarResena(socio, turno, puntuacion, comentario));
  }

  @Override
  public boolean socioYaReseno(Long turnoId) {
    return repositorioResena.existePorTurno(turnoId);
  }

  @Override
  public ResumenResenas resumenDeVeterinaria(Long veterinariaId) {
    return armarResumen(repositorioResena.listarPorVeterinaria(veterinariaId));
  }

  @Override
  public Map<Long, ResumenResenas> resumenesPorVeterinarias(List<Veterinaria> lista) {
    Map<Long, ResumenResenas> mapa = new HashMap<>();
    for (Veterinaria veterinaria : lista) {
      mapa.put(veterinaria.getId(), resumenDeVeterinaria(veterinaria.getId()));
    }
    return mapa;
  }

  @Override
  public List<Resena> listarPorVeterinaria(Long veterinariaId) {
    return repositorioResena.listarPorVeterinaria(veterinariaId);
  }

  @Override
  public Map<Long, List<Resena>> listarPorVeterinarias(List<Veterinaria> lista) {
    Map<Long, List<Resena>> mapa = new HashMap<>();
    for (Veterinaria veterinaria : lista) {
      mapa.put(veterinaria.getId(), listarPorVeterinaria(veterinaria.getId()));
    }
    return mapa;
  }

  private void exigirPuntuacion(int puntuacion) throws ResenaInvalida {
    if (puntuacion < PUNTUACION_MIN || puntuacion > PUNTUACION_MAX) {
      throw new ResenaInvalida("La puntuación debe ser entre 1 y 5");
    }
  }

  private void exigirComentario(String comentario) throws ResenaInvalida {
    if (comentario == null || comentario.isBlank()) {
      throw new ResenaInvalida("Escribí un comentario");
    }
  }

  private void exigirFinalizado(Turno turno) throws ResenaInvalida {
    if (!turno.estaFinalizado()) {
      throw new ResenaInvalida("Solo podés reseñar turnos que la clínica marcó como atendidos");
    }
  }

  private Turno buscarTurnoDelSocio(Socio socio, Long turnoId) throws ResenaInvalida {
    try {
      return servicioTurno.buscarTurnoDe(socio, turnoId);
    } catch (TurnoInvalido e) {
      throw new ResenaInvalida("No encontramos ese turno", e);
    }
  }

  private Resena armarResena(Socio socio, Turno turno, int puntuacion, String comentario) {
    Resena resena = new Resena();
    resena.setTurno(turno);
    resena.setVeterinaria(turno.getVeterinaria());
    resena.setSocio(socio);
    resena.setPuntuacion(puntuacion);
    resena.setComentario(comentario.trim());
    resena.setCreadoEn(LocalDateTime.now());
    return resena;
  }

  private ResumenResenas armarResumen(List<Resena> todas) {
    long cantidad = todas.size();
    double promedio = cantidad == 0
      ? 0
      : todas.stream().mapToInt(Resena::getPuntuacion).average().orElse(0);
    promedio = Math.round(promedio * FACTOR_DECIMAL) / FACTOR_DECIMAL;
    List<Resena> ultimas = cantidad <= MAX_ULTIMAS ? todas : todas.subList(0, MAX_ULTIMAS);
    return new ResumenResenas(promedio, cantidad, ultimas);
  }
}
