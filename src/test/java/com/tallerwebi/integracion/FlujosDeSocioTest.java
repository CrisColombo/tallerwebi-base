package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Resena;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.infraestructura.ClienteMercadoPago;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import com.tallerwebi.presentacion.ControladorLogin;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
public class FlujosDeSocioTest {

  private static final String URL_MERCADO_PAGO = "https://sandbox.mercadopago.com/checkout";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private ClienteMercadoPago clienteMercadoPago;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void publicarResenaDeberiaGuardarlaYListarlaEnVeterinarias() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, true, true);

    mockMvc
      .perform(
        post("/turnos/" + datos.turnoFinalizadoId + "/resena")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("puntuacion", "5")
          .param("comentario", "Muy buena atención")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/turnos?resenaOk=1"));

    List<Resena> guardadas = EscenarioSocio.enTransaccion(
      sessionFactory,
      transactionManager,
      session ->
        session
          .createQuery("from Resena r where r.turno.id = :id", Resena.class)
          .setParameter("id", datos.turnoFinalizadoId)
          .getResultList()
    );
    assertThat(guardadas, hasSize(1));
    assertThat(guardadas.get(0).getComentario(), equalTo("Muy buena atención"));

    MvcResult listado = mockMvc
      .perform(get("/veterinarias"))
      .andExpect(status().isOk())
      .andReturn();
    @SuppressWarnings("unchecked")
    Map<Long, List<Resena>> detalle = (Map<Long, List<Resena>>) listado
      .getModelAndView()
      .getModel()
      .get("resenasDetalle");
    boolean aparece = detalle
      .values()
      .stream()
      .flatMap(List::stream)
      .anyMatch(resena -> "Muy buena atención".equals(resena.getComentario()));
    assertThat(aparece, equalTo(true));
  }

  @Test
  public void resenaInvalidaDeberiaVolverATurnosConError() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, true, true);

    MvcResult result = mockMvc
      .perform(
        post("/turnos/" + datos.turnoFinalizadoId + "/resena")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("puntuacion", "4")
          .param("comentario", "   ")
      )
      .andExpect(status().is3xxRedirection())
      .andReturn();

    assertThat(result.getResponse().getRedirectedUrl(), containsString("errorResena="));
  }

  @Test
  public void pagarPlanDeberiaRedirigirAMercadoPago() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, false, false);
    when(clienteMercadoPago.crearPreferencia(anyString(), anyDouble(), anyString()))
      .thenReturn(URL_MERCADO_PAGO);

    mockMvc
      .perform(
        post("/mascotas/" + datos.mascotaId + "/pagar-plan")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("planId", String.valueOf(datos.planId))
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(URL_MERCADO_PAGO));
  }

  @Test
  public void pagarElMismoPlanAlDiaNoDeberiaIrAMercadoPago() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, true, false);

    mockMvc
      .perform(
        post("/mascotas/" + datos.mascotaId + "/pagar-plan")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("planId", String.valueOf(datos.planId))
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(
        redirectedUrl("/mascotas/" + datos.mascotaId + "?errorPago=Ya tenés este plan activo")
      );
  }

  @Test
  public void reservarTurnoConPlanVigenteDeberiaConfirmarlo() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, true, false);

    mockMvc
      .perform(
        post("/turnos")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("mascotaId", String.valueOf(datos.mascotaId))
          .param("veterinariaId", String.valueOf(datos.veterinariaId))
          .param("servicioId", String.valueOf(datos.servicioId))
          .param("fecha", EscenarioSocio.proximoDiaHabil().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .param("hora", "10:00")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/turnos"));

    List<Turno> turnos = EscenarioSocio.enTransaccion(
      sessionFactory,
      transactionManager,
      session ->
        session
          .createQuery("from Turno t where t.mascota.id = :id", Turno.class)
          .setParameter("id", datos.mascotaId)
          .getResultList()
    );
    assertThat(turnos, hasSize(1));
  }

  @Test
  public void reservarTurnoSinPlanDeberiaRechazarlo() throws Exception {
    EscenarioSocio datos = EscenarioSocio.cargar(sessionFactory, transactionManager, false, false);

    MvcResult result = mockMvc
      .perform(
        post("/turnos")
          .sessionAttr(ControladorLogin.USUARIO_ID, datos.usuarioId)
          .param("mascotaId", String.valueOf(datos.mascotaId))
          .param("veterinariaId", String.valueOf(datos.veterinariaId))
          .param("servicioId", String.valueOf(datos.servicioId))
          .param("fecha", EscenarioSocio.proximoDiaHabil().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .param("hora", "10:00")
      )
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getModelAndView().getViewName(), equalTo("nuevo-turno"));
    assertThat(
      String.valueOf(result.getModelAndView().getModel().get("error")),
      containsString("no tiene un plan vigente")
    );
  }
}
