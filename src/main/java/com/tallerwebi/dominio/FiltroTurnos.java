package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Criterios con los que el socio filtra "Mis turnos". Llegan como parámetros de la URL
 * (por ejemplo /turnos?vista=atendidos&amp;mascota=2&amp;sinResena=true), así el filtro se puede
 * guardar o compartir y el botón Atrás del navegador lo conserva.
 */
public class FiltroTurnos {

  public static final String PROXIMOS = "proximos";
  public static final String ATENDIDOS = "atendidos";
  public static final String CANCELADOS = "cancelados";
  public static final String TODOS = "todos";
  public static final String ASC = "asc";
  public static final String DESC = "desc";
  private static final List<String> VISTAS = List.of(PROXIMOS, ATENDIDOS, CANCELADOS, TODOS);

  private String vista = PROXIMOS;
  private List<Long> mascota = new ArrayList<>();
  private Especie especie;
  private Long veterinaria;
  private Long servicio;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate desde;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate hasta;

  private boolean sinResena;
  private String texto;
  private String orden;

  /** Aplica la pestaña y los filtros, y ordena: los próximos del más cercano al más lejano. */
  public List<Turno> aplicar(
    List<Turno> turnos,
    LocalDateTime ahora,
    Predicate<Turno> tieneResena
  ) {
    Comparator<Turno> porMomento = Comparator.comparing(FiltroTurnos::momento);
    return turnos
      .stream()
      .filter(t -> enVista(t, getVista(), ahora))
      .filter(t -> cumpleFiltros(t, tieneResena))
      .sorted(ASC.equals(getOrden()) ? porMomento : porMomento.reversed())
      .collect(Collectors.toList());
  }

  /** Cuántos turnos quedarían en cada pestaña con los filtros actuales. */
  public Map<String, Long> conteos(
    List<Turno> turnos,
    LocalDateTime ahora,
    Predicate<Turno> tieneResena
  ) {
    Map<String, Long> conteos = new LinkedHashMap<>();
    for (String v : VISTAS) {
      conteos.put(
        v,
        turnos.stream().filter(t -> enVista(t, v, ahora) && cumpleFiltros(t, tieneResena)).count()
      );
    }
    return conteos;
  }

  static boolean enVista(Turno turno, String vista, LocalDateTime ahora) {
    switch (vista) {
      case PROXIMOS:
        return turno.estaConfirmado() && !momento(turno).isBefore(ahora);
      case ATENDIDOS:
        return turno.estaFinalizado();
      case CANCELADOS:
        return turno.getEstado() == EstadoTurno.CANCELADO;
      default:
        return true;
    }
  }

  boolean cumpleFiltros(Turno turno, Predicate<Turno> tieneResena) {
    Mascota delTurno = turno.getMascota();
    return (
      (mascota.isEmpty() || mascota.contains(delTurno.getId())) &&
      (especie == null || especie == delTurno.getEspecie()) &&
      (veterinaria == null || Objects.equals(veterinaria, turno.getVeterinaria().getId())) &&
      (servicio == null || Objects.equals(servicio, turno.getServicio().getId())) &&
      (desde == null || !turno.getFecha().isBefore(desde)) &&
      (hasta == null || !turno.getFecha().isAfter(hasta)) &&
      (!sinResena || (turno.estaFinalizado() && !tieneResena.test(turno))) &&
      contieneTexto(turno)
    );
  }

  private boolean contieneTexto(Turno turno) {
    if (texto == null || texto.isBlank()) {
      return true;
    }
    String contenido = String.join(
      " ",
      turno.getMascota().getNombre(),
      turno.getVeterinaria().getNombre(),
      turno.getServicio().getNombre(),
      Objects.toString(turno.getObservacionesProfesional(), "")
    );
    return contenido.toLowerCase(Locale.ROOT).contains(texto.trim().toLowerCase(Locale.ROOT));
  }

  private static LocalDateTime momento(Turno turno) {
    return turno.getFecha().atTime(turno.getHora());
  }

  /** Si hay algún filtro aplicado además de la pestaña. */
  public boolean isActivo() {
    return (
      !mascota.isEmpty() ||
      especie != null ||
      veterinaria != null ||
      servicio != null ||
      desde != null ||
      hasta != null ||
      sinResena ||
      (texto != null && !texto.isBlank())
    );
  }

  public String getVista() {
    return VISTAS.contains(vista) ? vista : PROXIMOS;
  }

  public void setVista(String vista) {
    this.vista = vista;
  }

  public List<Long> getMascota() {
    return mascota;
  }

  public void setMascota(List<Long> mascota) {
    this.mascota = mascota == null ? new ArrayList<>() : mascota;
  }

  public Especie getEspecie() {
    return especie;
  }

  public void setEspecie(Especie especie) {
    this.especie = especie;
  }

  public Long getVeterinaria() {
    return veterinaria;
  }

  public void setVeterinaria(Long veterinaria) {
    this.veterinaria = veterinaria;
  }

  public Long getServicio() {
    return servicio;
  }

  public void setServicio(Long servicio) {
    this.servicio = servicio;
  }

  public LocalDate getDesde() {
    return desde;
  }

  public void setDesde(LocalDate desde) {
    this.desde = desde;
  }

  public LocalDate getHasta() {
    return hasta;
  }

  public void setHasta(LocalDate hasta) {
    this.hasta = hasta;
  }

  public boolean isSinResena() {
    return sinResena;
  }

  public void setSinResena(boolean sinResena) {
    this.sinResena = sinResena;
  }

  public String getTexto() {
    return texto;
  }

  public void setTexto(String texto) {
    this.texto = texto;
  }

  /** El orden elegido, o el natural de la pestaña si no se eligió ninguno válido. */
  public String getOrden() {
    if (ASC.equals(orden) || DESC.equals(orden)) {
      return orden;
    }
    return PROXIMOS.equals(getVista()) ? ASC : DESC;
  }

  public void setOrden(String orden) {
    this.orden = orden;
  }
}
