package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import java.util.List;

public interface ServicioReceta {
  void registrar(Turno turno, List<DatosReceta> recetas);
  List<RecetasDeConsulta> recetasDe(Socio socio, Long mascotaId) throws TurnoInvalido;
  void marcarComprada(Socio socio, Long recetaId) throws TurnoInvalido;
  List<RecetasDeConsulta> medicacionAnterior(Veterinaria veterinaria, Turno turno)
    throws TurnoInvalido;
}
