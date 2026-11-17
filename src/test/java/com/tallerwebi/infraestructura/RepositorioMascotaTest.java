package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Mascota;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

public class RepositorioMascotaTest {

  @Test
  public void listarDeberiaTraerLasMascotasOrdenadasPorNombre() {
    SessionFactory sessionFactory = mock(SessionFactory.class);
    Session sesion = mock(Session.class);
    @SuppressWarnings("unchecked")
    Query<Mascota> consulta = mock(Query.class);
    Mascota luna = new Mascota();
    when(sessionFactory.getCurrentSession()).thenReturn(sesion);
    when(sesion.createQuery(anyString(), eq(Mascota.class))).thenReturn(consulta);
    when(consulta.getResultList()).thenReturn(List.of(luna));
    RepositorioMascotaImpl repositorio = new RepositorioMascotaImpl(sessionFactory);

    assertThat(repositorio.listar(), contains(luna));
    verify(sesion).createQuery("select distinct e from Mascota e order by e.nombre", Mascota.class);
  }

  @Test
  public void guardarDeberiaPersistirLaMascotaEnLaSesion() {
    SessionFactory sessionFactory = mock(SessionFactory.class);
    Session sesion = mock(Session.class);
    when(sessionFactory.getCurrentSession()).thenReturn(sesion);
    Mascota luna = new Mascota();
    RepositorioMascotaImpl repositorio = new RepositorioMascotaImpl(sessionFactory);

    repositorio.guardar(luna);

    verify(sesion).persist(luna);
  }
}
