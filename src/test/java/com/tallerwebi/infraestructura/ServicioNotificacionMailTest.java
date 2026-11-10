package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.Mascota;
import com.tallerwebi.dominio.Servicio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.Veterinaria;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioNotificacionMailTest {

  private CapturaDeMail servicioNotificacion;
  private Turno turno;

  @BeforeEach
  public void init() {
    servicioNotificacion = new CapturaDeMail();
    turno = dadoUnTurno("Firulais");
  }

  @Test
  public void laConfirmacionDeberiaLlegarAlDestinatarioConLosDatosDelTurno() {
    servicioNotificacion.enviarConfirmacionTurno(turno, "socio@mail.com");

    assertThat(servicioNotificacion.destino, is("socio@mail.com"));
    assertThat(servicioNotificacion.asunto, is("Turno confirmado en Veterinaria San Justo"));
    assertThat(servicioNotificacion.html, containsString("¡Turno confirmado!"));
    assertThat(servicioNotificacion.html, containsString("Firulais"));
    assertThat(servicioNotificacion.html, containsString("Consulta clínica"));
    assertThat(servicioNotificacion.html, containsString("Veterinaria San Justo"));
    assertThat(servicioNotificacion.html, containsString("Calle 123"));
    assertThat(servicioNotificacion.html, containsString("5 de noviembre"));
    assertThat(servicioNotificacion.html, containsString("10:00"));
    assertThat(servicioNotificacion.html, containsString("#14b8a6"));
  }

  @Test
  public void laCancelacionDeberiaAvisarQueElTurnoFueCancelado() {
    servicioNotificacion.enviarCancelacionTurno(turno, "socio@mail.com");

    assertThat(servicioNotificacion.destino, is("socio@mail.com"));
    assertThat(servicioNotificacion.asunto, is("Turno cancelado en Veterinaria San Justo"));
    assertThat(servicioNotificacion.html, containsString("Turno cancelado"));
    assertThat(servicioNotificacion.html, containsString("El turno de Firulais fue cancelado"));
    assertThat(servicioNotificacion.html, containsString("#64748b"));
  }

  @Test
  public void elRecordatorioDeberiaAvisarQueSeAcercaElTurno() {
    servicioNotificacion.enviarRecordatorioTurno(turno, "socio@mail.com");

    assertThat(servicioNotificacion.destino, is("socio@mail.com"));
    assertThat(
      servicioNotificacion.asunto,
      is("Recordatorio de tu turno en Veterinaria San Justo")
    );
    assertThat(servicioNotificacion.html, containsString("Recordatorio de turno"));
    assertThat(servicioNotificacion.html, containsString("Se acerca el turno de Firulais"));
    assertThat(servicioNotificacion.html, containsString("#155e75"));
  }

  @Test
  public void elMailNoDeberiaDejarSinReemplazarNingunMarcador() {
    servicioNotificacion.enviarConfirmacionTurno(turno, "socio@mail.com");

    assertThat(servicioNotificacion.html, not(containsString("{{")));
  }

  @Test
  public void unNombreConCaracteresEspecialesNoDeberiaRomperElHtml() {
    servicioNotificacion.enviarConfirmacionTurno(dadoUnTurno("<b>Fido & Co</b>"), "socio@mail.com");

    assertThat(servicioNotificacion.html, containsString("&lt;b&gt;Fido &amp; Co&lt;/b&gt;"));
    assertThat(servicioNotificacion.html, not(containsString("<b>Fido")));
  }

  @Test
  public void unaVeterinariaSinDireccionNoDeberiaMostrarNull() {
    turno.getVeterinaria().setDireccion(null);

    servicioNotificacion.enviarConfirmacionTurno(turno, "socio@mail.com");

    assertThat(servicioNotificacion.html, not(containsString("null")));
  }

  @Test
  public void elMensajeDeberiaTenerRemitenteDestinatarioAsuntoYElLogoIncrustado() throws Exception {
    Session sesion = Session.getInstance(new Properties());

    MimeMessage mensaje = new ServicioNotificacionMail()
      .armarMensaje(
        sesion,
        "medipet@mail.com",
        "socio@mail.com",
        "Turno confirmado",
        "<p>Hola</p>"
      );

    InternetAddress remitente = (InternetAddress) mensaje.getFrom()[0];
    assertThat(remitente.getAddress(), is("medipet@mail.com"));
    assertThat(remitente.getPersonal(), is("MediPet"));
    assertThat(mensaje.getRecipients(Message.RecipientType.TO)[0].toString(), is("socio@mail.com"));
    assertThat(mensaje.getSubject(), is("Turno confirmado"));

    MimeMultipart contenido = (MimeMultipart) mensaje.getContent();
    assertThat(contenido.getCount(), is(2));
    assertThat(contenido.getBodyPart(0).getContent().toString(), is("<p>Hola</p>"));
    assertThat(contenido.getBodyPart(1).getHeader("Content-ID")[0], is("<logo>"));
  }

  @Test
  public void pedirUnaVariableDeEntornoQueNoExisteDeberiaAvisarCualFalta() {
    IllegalStateException error = assertThrows(
      IllegalStateException.class,
      () -> ServicioNotificacionMail.requerido("VARIABLE_QUE_NO_EXISTE_EN_MEDIPET")
    );

    assertThat(error.getMessage(), containsString("VARIABLE_QUE_NO_EXISTE_EN_MEDIPET"));
  }

  private Turno dadoUnTurno(String nombreMascota) {
    Mascota mascota = new Mascota();
    mascota.setNombre(nombreMascota);
    Servicio servicio = new Servicio();
    servicio.setNombre("Consulta clínica");
    Veterinaria veterinaria = new Veterinaria();
    veterinaria.setNombre("Veterinaria San Justo");
    veterinaria.setDireccion("Calle 123");

    Turno unTurno = new Turno();
    unTurno.setMascota(mascota);
    unTurno.setServicio(servicio);
    unTurno.setVeterinaria(veterinaria);
    unTurno.setFecha(LocalDate.of(2026, 11, 5));
    unTurno.setHora(LocalTime.of(10, 0));
    return unTurno;
  }

  // En vez de mandar el mail por SMTP, guarda lo que se iba a mandar.
  private static class CapturaDeMail extends ServicioNotificacionMail {

    private String destino;
    private String asunto;
    private String html;

    @Override
    void enviar(String emailDestino, String asuntoDelMail, String htmlDelMail) {
      this.destino = emailDestino;
      this.asunto = asuntoDelMail;
      this.html = htmlDelMail;
    }
  }
}
