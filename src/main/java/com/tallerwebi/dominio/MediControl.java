package com.tallerwebi.dominio;

import com.tallerwebi.infraestructura.ClienteGemini;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MediControl {

  public static final String COMENTARIO_OCULTO =
    "Comentario oculto por MediControl por no cumplir las normas de Medipet.";

  private static final String INSTRUCCION =
    """
    Sos MediControl, el moderador de reseñas de Medipet.
    Respondé solo SI o NO, sin otra palabra.
    SI si el texto tiene insultos, groserías, discriminación, amenazas,
    contenido sexual, datos personales de terceros o spam.
    NO si es una opinión normal de una atención veterinaria, aunque sea negativa.
    """;

  private final ClienteGemini clienteGemini;

  @Autowired
  public MediControl(ClienteGemini clienteGemini) {
    this.clienteGemini = clienteGemini;
  }

  public boolean debeCensurar(String comentario) {
    try {
      String respuesta = clienteGemini.completarConInstruccion(INSTRUCCION, comentario);
      return respondeSi(respuesta);
    } catch (Exception e) {
      return false;
    }
  }

  static boolean respondeSi(String respuesta) {
    if (respuesta == null || respuesta.isBlank()) return false;
    String token = respuesta.trim().toUpperCase(Locale.ROOT).split("\\s+")[0];
    token = token.replaceAll("[^A-ZÁÉÍÓÚÜÑ]", "");
    return "SI".equals(token) || "SÍ".equals(token);
  }
}
