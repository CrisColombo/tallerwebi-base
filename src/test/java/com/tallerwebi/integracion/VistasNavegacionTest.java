package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Especie;
import com.tallerwebi.dominio.EstadoTurno;
import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.Plan;
import com.tallerwebi.dominio.Receta;
import com.tallerwebi.dominio.Servicio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Veterinaria;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import com.tallerwebi.presentacion.ControladorLogin;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Renderiza las pantallas del rediseño con datos reales para detectar errores en las plantillas. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
public class VistasNavegacionTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;
  private Usuario usuarioSocio;
  private Usuario usuarioVeterinaria;
  private Mascota michi;
  private Turno proximo;
  private Turno atendido;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @Transactional
  @Rollback
  public void elInicioDelSocioDeberiaMostrarElProximoTurnoYLosPendientes() throws Exception {
    dadosLosDatosDeDemo();

    String html = pagina(comoSocio(get("/home")));

    assertThat(html, containsString("Tu próximo turno"));
    assertThat(html, containsString("Dermatología para Michi"));
    assertThat(html, containsString("finalizó la consulta de"));
    assertThat(html, containsString("Te falta comprar"));
    assertThat(html, containsString("Preguntale a MediBot"));
  }

  @Test
  @Transactional
  @Rollback
  public void misTurnosDeberiaMostrarLasPestanasYLosFiltros() throws Exception {
    dadosLosDatosDeDemo();

    String html = pagina(
      comoSocio(
        get("/turnos").param("vista", "atendidos").param("mascota", michi.getId().toString())
      )
    );

    assertThat(html, containsString("Atendidos"));
    assertThat(html, containsString("Dejar reseña"));
    assertThat(html, containsString("Solo sin reseña"));
  }

  @Test
  @Transactional
  @Rollback
  public void misTurnosConFiltrosSinResultadosDeberiaOfrecerLimpiarlos() throws Exception {
    dadosLosDatosDeDemo();

    String html = pagina(comoSocio(get("/turnos").param("texto", "nada que coincida")));

    assertThat(html, containsString("No hay turnos con estos filtros."));
  }

  @Test
  @Transactional
  @Rollback
  public void misMascotasYElPerfilDeberianMostrarseConSusPestanas() throws Exception {
    dadosLosDatosDeDemo();

    assertThat(pagina(comoSocio(get("/mascotas"))), containsString("Ver perfil"));
    String perfil = pagina(comoSocio(get("/mascotas/" + michi.getId())));

    assertThat(perfil, containsString("Última consulta"));
    assertThat(perfil, containsString("Historial clínico"));
    assertThat(perfil, containsString("¿Te conviene otro plan?"));
    assertThat(perfil, containsString("Pagar, renovar o cambiar de plan"));
  }

  @Test
  @Transactional
  @Rollback
  public void laAgendaDeLaVeterinariaDeberiaMostrarElDiaYLaFichaAlAtender() throws Exception {
    dadosLosDatosDeDemo();

    String agenda = pagina(comoVeterinaria(get("/veterinaria/inicio").param("periodo", "semana")));
    assertThat(agenda, containsString("Próximos 7 días"));
    assertThat(agenda, containsString("Michi"));
    assertThat(agenda, containsString("Atender"));

    String atender = pagina(
      comoVeterinaria(get("/veterinaria/turnos/" + proximo.getId() + "/finalizar"))
    );
    assertThat(atender, containsString("Medicación anterior"));
    assertThat(atender, containsString("Otomax"));
    assertThat(atender, containsString("Finalizar y enviar devolución"));
    assertThat(atender, containsString("Servicios adicionales realizados"));
    assertThat(atender, containsString("El socio debe pagar"));
    assertThat(atender, containsString("Cubierto"));
  }

  private String pagina(MockHttpServletRequestBuilder pedido) throws Exception {
    return mockMvc
      .perform(pedido)
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
  }

  private MockHttpServletRequestBuilder comoVeterinaria(MockHttpServletRequestBuilder pedido) {
    return pedido
      .sessionAttr(ControladorLogin.USUARIO_ID, usuarioVeterinaria.getId())
      .sessionAttr(ControladorLogin.ROL, Usuario.ROL_VETERINARIA);
  }

  private MockHttpServletRequestBuilder comoSocio(MockHttpServletRequestBuilder pedido) {
    return pedido
      .sessionAttr(ControladorLogin.USUARIO_ID, usuarioSocio.getId())
      .sessionAttr(ControladorLogin.ROL, "SOCIO");
  }

  private void dadosLosDatosDeDemo() {
    Session sesion = sessionFactory.getCurrentSession();
    usuarioSocio = dadoUnUsuario("lucia@test.com", "SOCIO");
    usuarioVeterinaria = dadoUnUsuario("vet@test.com", Usuario.ROL_VETERINARIA);

    Plan premium = new Plan();
    premium.setNombre("Plan Premium");
    premium.setDescripcion("Cobertura completa");
    premium.setPrecio(50000.0);
    premium.setNivel(2);
    sesion.persist(premium);

    Servicio dermatologia = new Servicio();
    dermatologia.setNombre("Dermatología");
    dermatologia.setNivelRequerido(2);
    dermatologia.setPrecioParticular(15000.0);
    sesion.persist(dermatologia);

    Veterinaria sanJusto = new Veterinaria();
    sanJusto.setNombre("Clínica Veterinaria San Justo");
    sanJusto.setDireccion("República de Chile 2915");
    sanJusto.setUsuario(usuarioVeterinaria);
    sanJusto.getServicios().add(dermatologia);
    sesion.persist(sanJusto);

    Socio socio = new Socio();
    socio.setNombre("Lucía");
    socio.setApellido("Gómez");
    socio.setDni("35111222");
    socio.setUsuario(usuarioSocio);
    michi = new Mascota();
    michi.setNombre("Michi");
    michi.setEspecie(Especie.GATO);
    michi.setFechaNacimiento(LocalDate.now().minusYears(5));
    michi.setPlan(premium);
    michi.setVenceSuscripcion(LocalDate.now().plusDays(4));
    socio.agregarMascota(michi);
    sesion.persist(socio);

    proximo =
      dadoUnTurno(sanJusto, dermatologia, LocalDate.now().plusDays(2), EstadoTurno.CONFIRMADO);
    atendido =
      dadoUnTurno(sanJusto, dermatologia, LocalDate.now().minusDays(5), EstadoTurno.FINALIZADO);
    atendido.finalizar("Otitis leve en el oído derecho.");
    sesion.persist(new Receta(atendido, "Otomax", "4 gotas cada 12 h"));
    sesion.flush();
  }

  private Usuario dadoUnUsuario(String email, String rol) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("x");
    usuario.setRol(rol);
    usuario.setActivo(true);
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }

  private Turno dadoUnTurno(
    Veterinaria veterinaria,
    Servicio servicio,
    LocalDate fecha,
    EstadoTurno estado
  ) {
    Turno turno = new Turno();
    turno.setMascota(michi);
    turno.setVeterinaria(veterinaria);
    turno.setServicio(servicio);
    turno.setFecha(fecha);
    turno.setHora(LocalTime.of(10, 0));
    turno.setEstado(estado);
    sessionFactory.getCurrentSession().persist(turno);
    return turno;
  }
}
