package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.EstadoTurno;
import com.tallerwebi.dominio.RepositorioTurno;
import com.tallerwebi.dominio.Turno;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioTurno")
public class RepositorioTurnoImpl extends RepositorioBase<Turno> implements RepositorioTurno {

  @Autowired
  public RepositorioTurnoImpl(SessionFactory sessionFactory) {
    super(sessionFactory, Turno.class);
  }

  @Override
  public void guardar(Turno turno) {
    sesion().persist(turno);
  }

  @Override
  public List<Turno> listarPorSocio(Long socioId) {
    return sesion()
      .createQuery(
        "from Turno t where t.mascota.socio.id = :socioId order by t.fecha desc, t.hora desc",
        Turno.class
      )
      .setParameter("socioId", socioId)
      .getResultList();
  }

  @Override
  public List<Turno> listarPorVeterinaria(Long veterinariaId) {
    return sesion()
      .createQuery(
        "from Turno t where t.veterinaria.id = :veterinariaId order by t.fecha, t.hora",
        Turno.class
      )
      .setParameter("veterinariaId", veterinariaId)
      .getResultList();
  }

  @Override
  public List<Turno> listarPorMascota(Long mascotaId) {
    return sesion()
      .createQuery(
        "from Turno t where t.mascota.id = :mascotaId order by t.fecha desc, t.hora desc",
        Turno.class
      )
      .setParameter("mascotaId", mascotaId)
      .getResultList();
  }

  @Override
  public List<LocalTime> horasOcupadas(Long veterinariaId, LocalDate fecha) {
    return sesion()
      .createQuery(
        "select t.hora from Turno t where t.veterinaria.id = :veterinariaId " +
        "and t.fecha = :fecha and t.estado = :estado",
        LocalTime.class
      )
      .setParameter("veterinariaId", veterinariaId)
      .setParameter("fecha", fecha)
      .setParameter("estado", EstadoTurno.CONFIRMADO)
      .getResultList();
  }

  @Override
  public List<Turno> listarConfirmadosSinRecordatorio(LocalDate desde, LocalDate hasta) {
    return sesion()
      .createQuery(
        "from Turno t where t.estado = :estado and t.recordatorioEnviado = false " +
        "and t.fecha between :desde and :hasta order by t.fecha, t.hora",
        Turno.class
      )
      .setParameter("estado", EstadoTurno.CONFIRMADO)
      .setParameter("desde", desde)
      .setParameter("hasta", hasta)
      .getResultList();
  }
}
