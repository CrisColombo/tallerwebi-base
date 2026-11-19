package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    CobroMensual cobro = cobroDelMes(turno);
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

  /** Cuenta las atenciones ya finalizadas de la mascota en el mes, para saber cuánto cupo queda. */
  private CobroMensual cobroDelMes(Turno turno) {
    Mascota mascota = turno.getMascota();
    YearMonth mes = YearMonth.from(turno.getFecha());
    CobroMensual cobro = new CobroMensual(mascota.getPlan(), 0);
    List<Turno> previos = repositorioTurno
      .listarPorMascota(mascota.getId())
      .stream()
      .filter(Turno::estaFinalizado)
      .filter(t -> !Objects.equals(t.getId(), turno.getId()))
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
