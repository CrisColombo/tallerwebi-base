package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.tallerwebi.dominio.Especie;
import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.Receta;
import com.tallerwebi.dominio.RepositorioReceta;
import com.tallerwebi.dominio.Servicio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Veterinaria;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HibernateInfraestructuraTestConfig.class })
public class RepositorioRecetaTest {

  private static final LocalDate FECHA = LocalDate.of(2026, 11, 5);

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioReceta repositorioReceta;
  private Veterinaria veterinaria;
  private Servicio servicio;

  @BeforeEach
  public void init() {
    repositorioReceta = new RepositorioRecetaImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void guardarDeberiaPersistirLaRecetaPendienteYPoderBuscarlaPorId() {
    Mascota mascota = dadaUnaMascotaDelSocio("30111222");
    dadaUnaVeterinariaConServicio();
    Receta receta = new Receta(guardarTurno(mascota, FECHA), "Amoxicilina", "Cada 12 horas");

    repositorioReceta.guardar(receta);

    Receta guardada = repositorioReceta.buscarPorId(receta.getId());
    assertThat(guardada, is(notNullValue()));
    assertThat(guardada.getMedicamento(), is("Amoxicilina"));
    assertThat(guardada.estaPendiente(), is(true));
  }

  @Test
  @Transactional
  @Rollback
  public void listarPorMascotaDeberiaDevolverSoloLasSuyasYLasDeLaConsultaMasRecientePrimero() {
    Mascota mia = dadaUnaMascotaDelSocio("30111222");
    Mascota ajena = dadaUnaMascotaDelSocio("30333444");
    dadaUnaVeterinariaConServicio();
    Receta vieja = new Receta(guardarTurno(mia, FECHA), "Antiparasitario", "");
    Receta reciente = new Receta(guardarTurno(mia, FECHA.plusDays(10)), "Amoxicilina", "");
    Receta deOtra = new Receta(guardarTurno(ajena, FECHA), "Meloxicam", "");
    repositorioReceta.guardar(vieja);
    repositorioReceta.guardar(reciente);
    repositorioReceta.guardar(deOtra);

    List<Receta> recetas = repositorioReceta.listarPorMascota(mia.getId());

    assertThat(recetas, contains(reciente, vieja));
  }

  @Test
  @Transactional
  public void sinRecetasLaMascotaNoTieneNingunaEnElListado() {
    assertThat(repositorioReceta.listarPorMascota(1L), is(empty()));
  }

  private Session sesion() {
    return sessionFactory.getCurrentSession();
  }

  private Mascota dadaUnaMascotaDelSocio(String dni) {
    Usuario usuario = new Usuario();
    usuario.setEmail(dni + "@test.com");
    Socio socio = new Socio();
    socio.setDni(dni);
    socio.setUsuario(usuario);
    Mascota mascota = new Mascota();
    mascota.setNombre("Firulais");
    mascota.setEspecie(Especie.PERRO);
    socio.agregarMascota(mascota);
    sesion().persist(socio);
    return mascota;
  }

  private void dadaUnaVeterinariaConServicio() {
    servicio = new Servicio();
    servicio.setNombre("Consulta clínica");
    servicio.setNivelRequerido(1);
    sesion().persist(servicio);
    veterinaria = new Veterinaria();
    veterinaria.setNombre("Veterinaria San Justo");
    veterinaria.getServicios().add(servicio);
    sesion().persist(veterinaria);
  }

  private Turno guardarTurno(Mascota mascota, LocalDate fecha) {
    Turno turno = new Turno();
    turno.setMascota(mascota);
    turno.setVeterinaria(veterinaria);
    turno.setServicio(servicio);
    turno.setFecha(fecha);
    turno.setHora(LocalTime.of(10, 0));
    sesion().persist(turno);
    return turno;
  }
}
