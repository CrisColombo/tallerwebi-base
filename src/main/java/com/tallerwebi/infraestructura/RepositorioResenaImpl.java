package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Resena;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioResena")
public class RepositorioResenaImpl extends RepositorioBase<Resena> implements RepositorioResena {

  @Autowired
  public RepositorioResenaImpl(SessionFactory sessionFactory) {
    super(sessionFactory, Resena.class);
  }

  @Override
  public void guardar(Resena resena) {
    sesion().save(resena);
  }

  @Override
  public boolean existePorTurno(Long turnoId) {
    String hql = "select count(r) from Resena r where r.turno.id = :turnoId";
    Long cantidad = sesion()
      .createQuery(hql, Long.class)
      .setParameter("turnoId", turnoId)
      .uniqueResult();
    return cantidad != null && cantidad > 0;
  }

  @Override
  public List<Resena> listarPorVeterinaria(Long veterinariaId) {
    String hql = "from Resena r where r.veterinaria.id = :vid order by r.creadoEn desc";
    return sesion()
      .createQuery(hql, Resena.class)
      .setParameter("vid", veterinariaId)
      .getResultList();
  }
}
