package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioReceta")
@Transactional
public class ServicioRecetaImpl implements ServicioReceta {

  private final RepositorioReceta repositorioReceta;
  private final Clock reloj;

  @Autowired
  public ServicioRecetaImpl(RepositorioReceta repositorioReceta) {
    this(repositorioReceta, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests.
  public ServicioRecetaImpl(RepositorioReceta repositorioReceta, Clock reloj) {
    this.repositorioReceta = repositorioReceta;
    this.reloj = reloj;
  }

  @Override
  public void registrar(Turno turno, List<DatosReceta> recetas) {
    if (recetas == null) {
      return;
    }
    for (DatosReceta datos : recetas) {
      String medicamento = limpiar(datos.getMedicamento());
      if (!medicamento.isEmpty()) {
        repositorioReceta.guardar(new Receta(turno, medicamento, limpiar(datos.getIndicaciones())));
      }
    }
  }

  @Override
  public List<RecetasDeConsulta> recetasDe(Socio socio, Long mascotaId) throws TurnoInvalido {
    if (socio.buscarMascota(mascotaId) == null) {
      throw new TurnoInvalido("No encontramos esa mascota");
    }
    return agruparPorConsulta(repositorioReceta.listarPorMascota(mascotaId));
  }

  @Override
  public List<RecetasDeConsulta> medicacionAnterior(Veterinaria veterinaria, Turno turno)
    throws TurnoInvalido {
    boolean esPropio =
      turno.getVeterinaria() != null &&
      Objects.equals(veterinaria.getId(), turno.getVeterinaria().getId());
    if (!esPropio || turno.getEstado() == EstadoTurno.CANCELADO) {
      throw new TurnoInvalido("No encontramos ese turno");
    }
    return agruparPorConsulta(repositorioReceta.listarPorMascota(turno.getMascota().getId()));
  }

  private static List<RecetasDeConsulta> agruparPorConsulta(List<Receta> recetas) {
    Map<Long, RecetasDeConsulta> porConsulta = new LinkedHashMap<>();
    for (Receta receta : recetas) {
      porConsulta
        .computeIfAbsent(receta.getTurno().getId(), id -> new RecetasDeConsulta(receta.getTurno()))
        .agregar(receta);
    }
    return new ArrayList<>(porConsulta.values());
  }

  @Override
  public void marcarComprada(Socio socio, Long recetaId) throws TurnoInvalido {
    Receta receta = recetaId == null ? null : repositorioReceta.buscarPorId(recetaId);
    if (receta == null || socio.buscarMascota(receta.getTurno().getMascota().getId()) == null) {
      throw new TurnoInvalido("No encontramos esa receta");
    }
    if (!receta.estaPendiente()) {
      throw new TurnoInvalido("Esa receta ya está marcada como comprada");
    }
    receta.marcarComprada(LocalDate.now(reloj));
  }

  private static String limpiar(String texto) {
    return texto == null ? "" : texto.trim();
  }
}
