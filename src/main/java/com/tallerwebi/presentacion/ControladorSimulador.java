package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioSimulador;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.SimulacionPlan;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorSimulador extends ControladorConSocio {

  private static final String REDIRECT_HOME = "redirect:/home";
  private static final int MESES_MAXIMO = 60;

  private final ServicioSimulador servicioSimulador;

  @Autowired
  public ControladorSimulador(ServicioSocio servicioSocio, ServicioSimulador servicioSimulador) {
    super(servicioSocio);
    this.servicioSimulador = servicioSimulador;
  }

  @RequestMapping(path = "/mascotas/{id}/simulador", method = RequestMethod.GET)
  public ModelAndView verSimulador(
    @PathVariable("id") Long id,
    @RequestParam(value = "meses", defaultValue = "12") int meses,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);
    if (socio == null) {
      return new ModelAndView(REDIRECT_HOME);
    }
    int periodo = Math.max(1, Math.min(meses, MESES_MAXIMO));
    try {
      SimulacionPlan simulacion = servicioSimulador.simular(socio, id, periodo);
      if (simulacion == null) {
        return new ModelAndView(REDIRECT_HOME);
      }
      Map<String, Object> modelo = new ModelMap();
      modelo.put("simulacion", simulacion);
      return new ModelAndView("simulador", modelo);
    } catch (TurnoInvalido e) {
      return new ModelAndView(REDIRECT_HOME);
    }
  }
}
