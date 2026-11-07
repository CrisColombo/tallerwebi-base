package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ResumenResenas;
import com.tallerwebi.dominio.ServicioCobertura;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioVeterinaria;
import com.tallerwebi.dominio.Veterinaria;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorVeterinaria {

  private final ServicioVeterinaria servicioVeterinaria;
  private final ServicioCobertura servicioCobertura;
  private final ServicioResena servicioResena;

  @Autowired
  public ControladorVeterinaria(
    ServicioVeterinaria servicioVeterinaria,
    ServicioCobertura servicioCobertura,
    ServicioResena servicioResena
  ) {
    this.servicioVeterinaria = servicioVeterinaria;
    this.servicioCobertura = servicioCobertura;
    this.servicioResena = servicioResena;
  }

  @RequestMapping(path = "/veterinarias", method = RequestMethod.GET)
  public ModelAndView listarVeterinarias(
    @RequestParam(name = "servicioId", required = false) Long servicioId,
    HttpSession sesion
  ) {
    if (
      com.tallerwebi.dominio.Usuario.ROL_VETERINARIA.equals(
        sesion.getAttribute(ControladorLogin.ROL)
      )
    ) {
      return new ModelAndView("redirect:/veterinaria/inicio");
    }
    List<Veterinaria> veterinarias = servicioVeterinaria.listar(servicioId);
    Map<Long, ResumenResenas> resenas = servicioResena.resumenesPorVeterinarias(veterinarias);

    Map<String, Object> modelo = new ModelMap();
    modelo.put("veterinarias", veterinarias);
    modelo.put("resenas", resenas);
    modelo.put("servicios", servicioCobertura.listarServicios());
    modelo.put("servicioId", servicioId);
    return new ModelAndView("veterinarias", modelo);
  }
}
