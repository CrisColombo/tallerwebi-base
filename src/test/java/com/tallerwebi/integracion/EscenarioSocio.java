package com.tallerwebi.integracion;

import com.tallerwebi.dominio.EstadoTurno;
import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.Plan;
import com.tallerwebi.dominio.Servicio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Veterinaria;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Datos mínimos en la base de test para los flujos del socio. */
class EscenarioSocio {

  final Long usuarioId;
  final Long mascotaId;
  final Long planId;
  final Long veterinariaId;
  final Long servicioId;
  final Long turnoFinalizadoId;

  private EscenarioSocio(
    Long usuarioId,
    Long mascotaId,
    Long planId,
    Long veterinariaId,
    Long servicioId,
    Long turnoFinalizadoId
  ) {
    this.usuarioId = usuarioId;
    this.mascotaId = mascotaId;
    this.planId = planId;
    this.veterinariaId = veterinariaId;
    this.servicioId = servicioId;
    this.turnoFinalizadoId = turnoFinalizadoId;
  }

  static EscenarioSocio cargar(
    SessionFactory sessionFactory,
    PlatformTransactionManager transactionManager,
    boolean conPlanVigente,
    boolean conTurnoFinalizado
  ) {
    return enTransaccion(
      sessionFactory,
      transactionManager,
      session -> {
        Plan plan = new Plan();
        plan.setNombre("Plan Básico");
        plan.setPrecio(20000.0);
        plan.setNivel(1);
        session.persist(plan);

        Servicio servicio = new Servicio();
        servicio.setNombre("Consulta clínica");
        servicio.setNivelRequerido(1);
        session.persist(servicio);

        Veterinaria veterinaria = new Veterinaria();
        veterinaria.setNombre("Clínica de prueba");
        veterinaria.setDireccion("Calle 1");
        veterinaria.setHoraApertura(LocalTime.of(9, 0));
        veterinaria.setHoraCierre(LocalTime.of(18, 0));
        veterinaria.getServicios().add(servicio);
        session.persist(veterinaria);

        Usuario usuario = new Usuario();
        usuario.setEmail("");
        usuario.setPassword("x");
        usuario.setRol("SOCIO");
        usuario.setActivo(true);

        Socio socio = new Socio();
        socio.setNombre("Lucía");
        socio.setApellido("Gómez");
        socio.setDni("DNI-" + System.nanoTime());
        socio.setUsuario(usuario);

        Mascota mascota = new Mascota();
        mascota.setNombre("Firulais");
        if (conPlanVigente) {
          mascota.setPlan(plan);
          mascota.setVenceSuscripcion(LocalDate.now().plusMonths(1));
        }
        socio.agregarMascota(mascota);
        session.persist(socio);
        session.flush();

        Long turnoId = null;
        if (conTurnoFinalizado) {
          Turno turno = new Turno();
          turno.setMascota(mascota);
          turno.setVeterinaria(veterinaria);
          turno.setServicio(servicio);
          turno.setFecha(proximoDiaHabil());
          turno.setHora(LocalTime.of(10, 0));
          turno.setEstado(EstadoTurno.FINALIZADO);
          session.persist(turno);
          session.flush();
          turnoId = turno.getId();
        }

        return new EscenarioSocio(
          usuario.getId(),
          mascota.getId(),
          plan.getId(),
          veterinaria.getId(),
          servicio.getId(),
          turnoId
        );
      }
    );
  }

  static LocalDate proximoDiaHabil() {
    LocalDate fecha = LocalDate.now().plusDays(1);
    while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
      fecha = fecha.plusDays(1);
    }
    return fecha;
  }

  static <T> T enTransaccion(
    SessionFactory sessionFactory,
    PlatformTransactionManager transactionManager,
    Function<Session, T> trabajo
  ) {
    return new TransactionTemplate(transactionManager)
      .execute(status -> trabajo.apply(sessionFactory.getCurrentSession()));
  }
}
