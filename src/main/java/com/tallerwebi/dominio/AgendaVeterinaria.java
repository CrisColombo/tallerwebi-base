package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** El día de trabajo de una veterinaria: los turnos del período elegido y los que quedaron sin cerrar. */
public class AgendaVeterinaria {

  public static final String HOY = "hoy";
  public static final String MANANA = "manana";
  public static final String SEMANA = "semana";
  public static final String TODOS = "todos";
  private static final List<String> PERIODOS = List.of(HOY, MANANA, SEMANA, TODOS);
  private static final int DIAS_SEMANA = 7;

  private final String periodo;
  private final LocalDateTime ahora;
  private final List<Turno> turnos;
  private final List<Turno> sinCerrar;

  public AgendaVeterinaria(List<Turno> todos, String periodo, String buscar, LocalDateTime ahora) {
    this.periodo = PERIODOS.contains(periodo) ? periodo : HOY;
    this.ahora = ahora;
    LocalDate hoy = ahora.toLocalDate();
    Comparator<Turno> porMomento = Comparator.comparing(AgendaVeterinaria::momento);
    this.turnos =
      todos
        .stream()
        .filter(t -> enPeriodo(t, hoy))
        .filter(t -> coincide(t, buscar))
        .sorted(TODOS.equals(this.periodo) ? porMomento.reversed() : porMomento)
        .collect(Collectors.toList());
    this.sinCerrar =
      todos
        .stream()
        .filter(t -> t.estaConfirmado() && t.getFecha().isBefore(hoy))
        .sorted(porMomento)
        .collect(Collectors.toList());
  }

  private boolean enPeriodo(Turno turno, LocalDate hoy) {
    LocalDate fecha = turno.getFecha();
    switch (periodo) {
      case HOY:
        return fecha.isEqual(hoy);
      case MANANA:
        return fecha.isEqual(hoy.plusDays(1));
      case SEMANA:
        return !fecha.isBefore(hoy) && fecha.isBefore(hoy.plusDays(DIAS_SEMANA));
      default:
        return true;
    }
  }

  private static boolean coincide(Turno turno, String buscar) {
    if (buscar == null || buscar.isBlank()) {
      return true;
    }
    Socio socio = turno.getMascota().getSocio();
    String contenido = String.join(
      " ",
      turno.getMascota().getNombre(),
      socio == null ? "" : socio.getNombre() + " " + socio.getApellido()
    );
    return contenido.toLowerCase(Locale.ROOT).contains(buscar.trim().toLowerCase(Locale.ROOT));
  }

  private static LocalDateTime momento(Turno turno) {
    return turno.getFecha().atTime(turno.getHora());
  }

  /** El primer turno confirmado que todavía no empezó: es el que se resalta como "sigue". */
  public Turno getSiguiente() {
    return turnos
      .stream()
      .filter(t -> t.estaConfirmado() && !momento(t).isBefore(ahora))
      .min(Comparator.comparing(AgendaVeterinaria::momento))
      .orElse(null);
  }

  public long getAtendidos() {
    return turnos.stream().filter(Turno::estaFinalizado).count();
  }

  public String getPeriodo() {
    return periodo;
  }

  public LocalDate getHoy() {
    return ahora.toLocalDate();
  }

  public List<Turno> getTurnos() {
    return turnos;
  }

  public List<Turno> getSinCerrar() {
    return sinCerrar;
  }
}
