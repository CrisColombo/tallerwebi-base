package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorResena extends ControladorConSocio {

  private static final String REDIRECT_TURNOS = "redirect:/turnos";

  private final ServicioResena servicioResena;

  @Autowired
  public ControladorResena(ServicioSocio servicioSocio, ServicioResena servicioResena) {
    super(servicioSocio);
    this.servicioResena = servicioResena;
  }

  @RequestMapping(path = "/turnos/{id}/resena", method = RequestMethod.POST)
  public ModelAndView crearResena(
    @PathVariable("id") Long turnoId,
    @RequestParam("puntuacion") int puntuacion,
    @RequestParam("comentario") String comentario,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);

    if (socio == null) {
      return new ModelAndView("redirect:/login");
    }

    try {
      servicioResena.crearResena(socio, turnoId, puntuacion, comentario);
      return new ModelAndView(REDIRECT_TURNOS + "?resenaOk=1");
    } catch (ResenaInvalida e) {
      String err = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
      return new ModelAndView(REDIRECT_TURNOS + "?errorResena=" + err);
    }
  }
}
