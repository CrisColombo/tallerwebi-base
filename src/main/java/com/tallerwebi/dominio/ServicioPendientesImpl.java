package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPendientes")
@Transactional
public class ServicioPendientesImpl implements ServicioPendientes {

  /** Días en que una consulta finalizada se muestra como novedad en el Inicio. */
  static final int DIAS_CONSULTA_RECIENTE = 15;
  /** Días de anticipación con que se avisa que un plan vence. */
  static final int DIAS_AVISO_VENCIMIENTO = 7;
  /** Días durante los que se pide la reseña de una consulta; después ya no se insiste. */
  static final int DIAS_PEDIR_RESENA = 60;

  private final RepositorioTurno repositorioTurno;
  private final RepositorioReceta repositorioReceta;
  private final ServicioResena servicioResena;
  private final Clock reloj;

  @Autowired
  public ServicioPendientesImpl(
    RepositorioTurno repositorioTurno,
    RepositorioReceta repositorioReceta,
    ServicioResena servicioResena
  ) {
    this(repositorioTurno, repositorioReceta, servicioResena, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests.
  public ServicioPendientesImpl(
    RepositorioTurno repositorioTurno,
    RepositorioReceta repositorioReceta,
    ServicioResena servicioResena,
    Clock reloj
  ) {
    this.repositorioTurno = repositorioTurno;
    this.repositorioReceta = repositorioReceta;
    this.servicioResena = servicioResena;
    this.reloj = reloj;
  }

  @Override
  public Pendientes pendientesDe(Socio socio) {
    List<Turno> turnos = repositorioTurno.listarPorSocio(socio.getId());
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDate hoy = ahora.toLocalDate();

    List<Turno> proximos = turnos
      .stream()
      .filter(t -> t.estaConfirmado() && momento(t).isAfter(ahora))
      .sorted(Comparator.comparing(this::momento))
      .collect(Collectors.toList());

    LocalDate limiteResena = hoy.minusDays(DIAS_PEDIR_RESENA);
    List<Turno> sinResena = turnos
      .stream()
      .filter(Turno::estaFinalizado)
      .filter(t -> !t.getFecha().isBefore(limiteResena))
      .filter(t -> !servicioResena.socioYaReseno(t.getId()))
      .sorted(Comparator.comparing(this::momento).reversed())
      .collect(Collectors.toList());

    LocalDate desde = hoy.minusDays(DIAS_CONSULTA_RECIENTE);
    List<Turno> recientes = sinResena
      .stream()
      .filter(t -> !t.getFecha().isBefore(desde))
      .collect(Collectors.toList());
    // Una consulta reciente ya ofrece su reseña: no se repite en la lista de reseñas.
    Set<Turno> yaMostrados = Set.copyOf(recientes);
    List<Turno> resenasViejas = sinResena
      .stream()
      .filter(t -> !yaMostrados.contains(t))
      .collect(Collectors.toList());

    return new Pendientes(
      proximos,
      recientes,
      resenasViejas,
      recetasSinComprar(socio),
      planesPorVencer(socio, hoy)
    );
  }

  private List<Receta> recetasSinComprar(Socio socio) {
    return socio
      .getMascotas()
      .stream()
      .flatMap(m -> repositorioReceta.listarPorMascota(m.getId()).stream())
      .filter(Receta::estaPendiente)
      .collect(Collectors.toList());
  }

  private List<Mascota> planesPorVencer(Socio socio, LocalDate hoy) {
    LocalDate limite = hoy.plusDays(DIAS_AVISO_VENCIMIENTO);
    return socio
      .getMascotas()
      .stream()
      .filter(m -> m.getPlan() != null)
      .filter(m -> m.getVenceSuscripcion() == null || !m.getVenceSuscripcion().isAfter(limite))
      .collect(Collectors.toList());
  }

  private LocalDateTime momento(Turno turno) {
    return turno.getFecha().atTime(turno.getHora());
  }
}
