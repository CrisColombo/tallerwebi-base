package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.ServicioNotificacion;
import com.tallerwebi.dominio.Turno;
import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class ServicioNotificacionMail implements ServicioNotificacion {

  private static final Locale LOCALE = Locale.forLanguageTag("es-AR");
  private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", LOCALE);
  private static final DateTimeFormatter DIA_Y_MES = DateTimeFormatter.ofPattern(
    "d 'de' MMMM",
    LOCALE
  );
  private static final DateTimeFormatter ANIO = DateTimeFormatter.ofPattern("yyyy");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  @Override
  public void enviarConfirmacionTurno(Turno turno, String emailDestino) {
    String usuario = requerido("MAIL_USER");
    String clave = requerido("MAIL_PASSWORD");

    Properties props = new Properties();
    props.put("mail.smtp.host", requerido("MAIL_HOST"));
    props.put("mail.smtp.port", requerido("MAIL_PORT"));
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "5000");
    props.put("mail.smtp.writetimeout", "5000");

    Session sesion = Session.getInstance(
      props,
      new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(usuario, clave);
        }
      }
    );

    try {
      MimeMessage mensaje = new MimeMessage(sesion);
      mensaje.setFrom(new InternetAddress(usuario, "MediPet"));
      mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestino));
      mensaje.setSubject("Turno confirmado en " + turno.getVeterinaria().getNombre(), "UTF-8");

      MimeMultipart contenido = new MimeMultipart("related");

      MimeBodyPart parteHtml = new MimeBodyPart();
      parteHtml.setContent(armarHtml(turno), "text/html; charset=UTF-8");
      contenido.addBodyPart(parteHtml);

      MimeBodyPart parteLogo = new MimeBodyPart();
      parteLogo.setDataHandler(new DataHandler(new ByteArrayDataSource(leerLogo(), "image/png")));
      parteLogo.setContentID("<logo>");
      parteLogo.setDisposition(MimeBodyPart.INLINE);
      contenido.addBodyPart(parteLogo);

      mensaje.setContent(contenido);
      Transport.send(mensaje);
    } catch (MessagingException | IOException e) {
      throw new IllegalStateException("No se pudo enviar el mail de confirmación", e);
    }
  }

  private byte[] leerLogo() throws IOException {
    try (var logo = getClass().getResourceAsStream("/mail/logo.png")) {
      if (logo == null) {
        throw new IOException("No se encontró /mail/logo.png");
      }
      return logo.readAllBytes();
    }
  }

  private String armarHtml(Turno turno) {
    String plantilla =
      """
      <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0fdfa;font-family:Arial,sans-serif;">
        <tr>
          <td align="center" style="padding:24px 12px;">

            <table cellpadding="0" cellspacing="0">
              <tr>
                <td align="center" width="72" height="72" style="background:#ffffff;border-radius:36px;">
                  <img src="cid:logo" width="56" height="56" alt="MediPet" style="display:block;margin:0 auto;">
                </td>
              </tr>
            </table>

            <div style="font-size:21px;font-weight:bold;color:#155e75;margin:10px 0 2px;">¡Turno confirmado!</div>
            <div style="font-size:14px;color:#5b7c7a;margin-bottom:16px;">Te esperamos con {{mascota}}</div>

            <table width="420" cellpadding="0" cellspacing="0" style="max-width:100%;background:#ffffff;border:1px solid #cdeee9;border-radius:12px;">
              <tr>
                <td style="background:#14b8a6;padding:16px 18px;border-radius:12px 12px 0 0;">
                  <table width="100%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="color:#ffffff;">
                        <div style="font-size:11px;color:#d7f7f2;">{{diaSemana}}</div>
                        <div style="font-size:22px;font-weight:bold;">{{diaYMes}}</div>
                        <div style="font-size:12px;color:#d7f7f2;">{{anio}}</div>
                      </td>
                      <td align="right">
                        <span style="background:#fb7185;color:#ffffff;font-size:20px;font-weight:bold;padding:8px 14px;border-radius:8px;">{{hora}}</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td style="padding:16px 18px;font-size:14px;line-height:1.7;color:#134e4a;">
                  <span style="color:#5b7c7a;">Servicio</span><br><b>{{servicio}}</b><br><br>
                  <span style="color:#5b7c7a;">Veterinaria</span><br><b>{{veterinaria}}</b><br><br>
                  <span style="color:#5b7c7a;">Dirección</span><br><b>{{direccion}}</b>
                </td>
              </tr>
            </table>

            <div style="font-size:12px;color:#5b7c7a;margin-top:14px;">Gracias por usar MediPet</div>
          </td>
        </tr>
      </table>
      """;

    return plantilla
      .replace("{{mascota}}", escapar(turno.getMascota().getNombre()))
      .replace("{{diaSemana}}", turno.getFecha().format(DIA_SEMANA).toUpperCase(LOCALE))
      .replace("{{diaYMes}}", turno.getFecha().format(DIA_Y_MES))
      .replace("{{anio}}", turno.getFecha().format(ANIO))
      .replace("{{hora}}", turno.getHora().format(HORA))
      .replace("{{servicio}}", escapar(turno.getServicio().getNombre()))
      .replace("{{veterinaria}}", escapar(turno.getVeterinaria().getNombre()))
      .replace("{{direccion}}", escapar(turno.getVeterinaria().getDireccion()));
  }

  // Evita que un nombre con < o & rompa el HTML del mail.
  private static String escapar(String texto) {
    return texto == null
      ? ""
      : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String requerido(String nombre) {
    String valor = System.getenv(nombre);
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException("Definí " + nombre + " en el entorno");
    }
    return valor;
  }
}
