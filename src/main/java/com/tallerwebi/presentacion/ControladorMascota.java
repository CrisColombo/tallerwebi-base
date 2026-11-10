package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.*;
import com.tallerwebi.dominio.excepcion.DatosDeRegistroInvalidos;
import com.tallerwebi.dominio.excepcion.PagoRechazado;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorMascota extends ControladorConSocio {

  private ServicioPlan servicioPlan;
  private ServicioMascota servicioMascota;
  private ServicioTurno servicioTurno;
  private ServicioPago servicioPago;

  private static final String REDIRECT_HOME = "redirect:/home";

  @Autowired
  public ControladorMascota(
    ServicioSocio servicioSocio,
    ServicioPlan servicioPlan,
    ServicioMascota servicioMascota,
    ServicioTurno servicioTurno,
    ServicioPago servicioPago
  ) {
    super(servicioSocio);
    this.servicioPlan = servicioPlan;
    this.servicioMascota = servicioMascota;
    this.servicioTurno = servicioTurno;
    this.servicioPago = servicioPago;
  }

  @RequestMapping(path = "/mascotas/nueva", method = RequestMethod.GET)
  public ModelAndView nuevaMascota(HttpSession sesion) {
    Socio socio = socioLogueado(sesion);
    if (socio == null) {
      return new ModelAndView(REDIRECT_HOME);
    }
    return vistaNuevaMascota(new Mascota(), null, null);
  }

  @RequestMapping(path = "/mascotas", method = RequestMethod.POST)
  public ModelAndView agregarMascota(
    @ModelAttribute("mascota") Mascota mascota,
    @RequestParam(value = "planId", required = false) Long planId,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);
    if (socio == null) {
      return new ModelAndView(REDIRECT_HOME);
    }
    Mascota creada;
    try {
      creada = servicioMascota.agregarMascota(socio, mascota, planId);
    } catch (DatosDeRegistroInvalidos e) {
      return vistaNuevaMascota(mascota, planId, e.getMessage());
    }
    try {
      String urlMercadoPago = servicioPago.iniciarPagoPlan(socio, creada.getId(), planId);
      return new ModelAndView("redirect:" + urlMercadoPago);
    } catch (PagoRechazado e) {
      String mensaje = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
      return new ModelAndView("redirect:/mascotas/" + creada.getId() + "?errorPago=" + mensaje);
    }
  }

  private ModelAndView vistaNuevaMascota(Mascota mascota, Long planId, String error) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("mascota", mascota);
    modelo.put("especies", Especie.values());
    modelo.put("planes", servicioPlan.listar());
    modelo.put("planId", planId);
    if (error != null) {
      modelo.put("error", error);
    }
    return new ModelAndView("nueva-mascota", modelo);
  }

  @RequestMapping(path = "/mascotas/{id}", method = RequestMethod.GET)
  public ModelAndView verPerfil(@PathVariable("id") Long id, HttpSession sesion) {
    Socio socio = socioLogueado(sesion);
    Mascota mascota = servicioMascota.buscarPorIdParaSocio(socio, id);
    if (mascota == null) {
      return new ModelAndView("redirect:/home");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("mascota", mascota);
    modelo.put("planes", servicioPlan.listar());
    try {
      HistorialTurnos historial = servicioTurno.historialDe(socio, id);
      modelo.put("historial", historial);
    } catch (TurnoInvalido e) {
      return new ModelAndView("redirect:/home");
    }
    return new ModelAndView("mascota-perfil", modelo);
  }

  @RequestMapping(path = "/mascotas/{id}/baja", method = RequestMethod.POST)
  public ModelAndView darDeBaja(@PathVariable("id") Long id, HttpSession sesion) {
    servicioMascota.darDeBaja(socioLogueado(sesion), id);
    return new ModelAndView("redirect:/mascotas/" + id);
  }
}
