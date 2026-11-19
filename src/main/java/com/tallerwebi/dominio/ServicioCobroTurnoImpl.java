package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioCobroTurno")
@Transactional
public class ServicioCobroTurnoImpl implements ServicioCobroTurno {

  private final ServicioTurno servicioTurno;
  private final RepositorioTurno repositorioTurno;
  private final RepositorioServicio repositorioServicio;

  @Autowired
  public ServicioCobroTurnoImpl(
    ServicioTurno servicioTurno,
    RepositorioTurno repositorioTurno,
    RepositorioServicio repositorioServicio
  ) {
    this.servicioTurno = servicioTurno;
    this.repositorioTurno = repositorioTurno;
    this.repositorioServicio = repositorioServicio;
  }

  @Override
  public CobroTurno calcularCobro(
    Veterinaria veterinaria,
    Long turnoId,
    List<Long> serviciosAdicionalesIds
  ) throws TurnoInvalido {
    Turno turno = servicioTurno.buscarTurnoDeVeterinaria(veterinaria, turnoId);
    exigirPrecio(turno.getServicio());
    List<Servicio> servicios = new ArrayList<>();
    servicios.add(turno.getServicio());
    servicios.addAll(serviciosAdicionales(veterinaria, serviciosAdicionalesIds));

    CobroMensual cobro = cobroDelMes(
      turno.getMascota(),
      YearMonth.from(turno.getFecha()),
      turno.getId(),
      Turno::estaFinalizado
    );
    List<LineaCobro> lineas = new ArrayList<>();
    for (Servicio servicio : servicios) {
      double precio = servicio.getPrecioParticular();
      lineas.add(new LineaCobro(servicio.getNombre(), precio, cobro.cobrar(servicio, precio)));
    }
    return new CobroTurno(lineas);
  }

  @Override
  public void finalizarConAdicionales(
    Veterinaria veterinaria,
    Long turnoId,
    String observaciones,
    List<Long> serviciosAdicionalesIds
  ) throws TurnoInvalido {
    Turno turno = servicioTurno.buscarTurnoDeVeterinaria(veterinaria, turnoId);
    if (!turno.estaConfirmado()) {
      throw new TurnoInvalido("Solo se pueden finalizar turnos confirmados");
    }
    double precioPrincipal = exigirPrecio(turno.getServicio());
    List<Servicio> adicionales = serviciosAdicionales(veterinaria, serviciosAdicionalesIds);

    turno.finalizar(observaciones);
    turno.setPrecioParticular(precioPrincipal);
    for (Servicio adicional : adicionales) {
      turno.agregarAdicional(adicional, adicional.getPrecioParticular());
    }
  }

  /** Servicios extra elegidos: tienen que ser de la veterinaria y tener precio de catálogo. */
  private List<Servicio> serviciosAdicionales(Veterinaria veterinaria, List<Long> ids)
    throws TurnoInvalido {
    List<Servicio> servicios = new ArrayList<>();
    if (ids == null) {
      return servicios;
    }
    for (Long id : ids) {
      Servicio servicio = id == null ? null : repositorioServicio.buscarPorId(id);
      if (servicio == null || !veterinaria.ofrece(servicio)) {
        throw new TurnoInvalido("La veterinaria no ofrece ese servicio");
      }
      exigirPrecio(servicio);
      servicios.add(servicio);
    }
    return servicios;
  }

  private double exigirPrecio(Servicio servicio) throws TurnoInvalido {
    if (servicio.getPrecioParticular() == null) {
      throw new TurnoInvalido(servicio.getNombre() + " no tiene un precio cargado");
    }
    return servicio.getPrecioParticular();
  }

  @Override
  public AvisoCobro avisoPara(Mascota mascota, Long servicioId, LocalDate fecha) {
    Servicio servicio = servicioId == null ? null : repositorioServicio.buscarPorId(servicioId);
    if (
      mascota == null ||
      mascota.getPlan() == null ||
      servicio == null ||
      servicio.getPrecioParticular() == null
    ) {
      return null;
    }
    YearMonth mes = YearMonth.from(fecha == null ? LocalDate.now() : fecha);
    CobroMensual cobro = cobroDelMes(
      mascota,
      mes,
      null,
      t -> t.estaFinalizado() || t.estaConfirmado()
    );
    int usadas = cobro.getCubiertas();
    double precio = servicio.getPrecioParticular();
    double paga = cobro.cobrar(servicio, precio);
    boolean cubierta = cobro.getCubiertas() > usadas;
    return redactarAviso(mascota.getPlan(), cubierta, usadas, precio, paga);
  }

  private AvisoCobro redactarAviso(
    Plan plan,
    boolean cubierta,
    int usadas,
    double precio,
    double paga
  ) {
    Number tope = plan.getTopeMensual();
    if (!cubierta) {
      return new AvisoCobro(
        "Tu plan no cubre este servicio. Pagás el precio particular: " + dinero(precio) + ".",
        true
      );
    }
    if (tope == null) {
      return new AvisoCobro("Tu plan cubre esta atención.", false);
    }
    if (usadas >= tope.intValue()) {
      return new AvisoCobro(
        "Ya usaste las " +
        tope.intValue() +
        " atenciones de este mes. Esta la pagás a precio particular: " +
        dinero(precio) +
        ".",
        true
      );
    }
    int quedan = tope.intValue() - usadas;
    String restantes = quedan == 1 ? "Te queda 1 atención" : "Te quedan " + quedan + " atenciones";
    String pago = paga == 0
      ? "Esta no tiene costo para vos."
      : "Pagás un copago de " + dinero(paga) + ".";
    return new AvisoCobro(
      restantes + " este mes con tu plan " + plan.getNombre() + ". " + pago,
      false
    );
  }

  private String dinero(double monto) {
    return "$" + String.format(Locale.of("es", "AR"), "%,.0f", monto);
  }

  /** Cuenta las atenciones de la mascota en el mes para saber cuánto cupo del plan queda. */
  private CobroMensual cobroDelMes(
    Mascota mascota,
    YearMonth mes,
    Long turnoExcluidoId,
    Predicate<Turno> cuenta
  ) {
    CobroMensual cobro = new CobroMensual(mascota.getPlan(), 0);
    List<Turno> previos = repositorioTurno
      .listarPorMascota(mascota.getId())
      .stream()
      .filter(cuenta)
      .filter(t -> turnoExcluidoId == null || !Objects.equals(t.getId(), turnoExcluidoId))
      .filter(t -> YearMonth.from(t.getFecha()).equals(mes))
      .collect(Collectors.toList());
    for (Turno previo : previos) {
      cobro.registrar(previo.getServicio());
      for (AtencionAdicional adicional : previo.getAdicionales()) {
        cobro.registrar(adicional.getServicio());
      }
    }
    return cobro;
  }
}
