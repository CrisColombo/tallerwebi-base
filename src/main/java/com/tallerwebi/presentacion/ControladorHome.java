package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPendientes;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorHome extends ControladorConSocio {

  private static final String SOCIO = "socio";
  private static final String PENDIENTES = "pendientes";

  private final ServicioPendientes servicioPendientes;

  @Autowired
  public ControladorHome(ServicioSocio servicioSocio, ServicioPendientes servicioPendientes) {
    super(servicioSocio);
    this.servicioPendientes = servicioPendientes;
  }

  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView irAHome(HttpSession sesion) {
    if (Usuario.ROL_VETERINARIA.equals(sesion.getAttribute(ControladorLogin.ROL))) {
      return new ModelAndView("redirect:/veterinaria/inicio");
    }
    Map<String, Object> modelo = new ModelMap();
    Socio socio = socioLogueado(sesion);
    modelo.put(SOCIO, socio);
    if (socio != null) {
      modelo.put(PENDIENTES, servicioPendientes.pendientesDe(socio));
    }
    return new ModelAndView("home", modelo);
  }

  @RequestMapping(path = "/mascotas", method = RequestMethod.GET)
  public ModelAndView misMascotas(HttpSession sesion) {
    Socio socio = socioLogueado(sesion);
    if (socio == null) {
      return new ModelAndView("redirect:/home");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put(SOCIO, socio);
    modelo.put(PENDIENTES, servicioPendientes.pendientesDe(socio));
    return new ModelAndView("mis-mascotas", modelo);
  }
}
