package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Receta;
import com.tallerwebi.dominio.RepositorioReceta;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioReceta")
public class RepositorioRecetaImpl extends RepositorioBase<Receta> implements RepositorioReceta {

  @Autowired
  public RepositorioRecetaImpl(SessionFactory sessionFactory) {
    super(sessionFactory, Receta.class);
  }

  @Override
  public void guardar(Receta receta) {
    sesion().persist(receta);
  }

  @Override
  public List<Receta> listarPorMascota(Long mascotaId) {
    return sesion()
      .createQuery(
        "from Receta r where r.turno.mascota.id = :mascotaId " +
        "order by r.turno.fecha desc, r.turno.hora desc, r.id",
        Receta.class
      )
      .setParameter("mascotaId", mascotaId)
      .getResultList();
  }
}
