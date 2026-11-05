package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.PagoRechazado;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPago extends ControladorConSocio {

  private final ServicioPago servicioPago;

  @Autowired
  public ControladorPago(ServicioSocio servicioSocio, ServicioPago servicioPago) {
    super(servicioSocio);
    this.servicioPago = servicioPago;
  }

  @RequestMapping(path = "/mascotas/{id}/pagar-plan", method = RequestMethod.POST)
  public ModelAndView pagarPlan(
    @PathVariable("id") Long mascotaId,
    @RequestParam("planId") Long planId,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);

    if (socio == null) {
      return new ModelAndView("redirect:/login");
    }

    try {
      String urlMercadoPago = servicioPago.iniciarPagoPlan(socio, mascotaId, planId);
      return new ModelAndView("redirect:" + urlMercadoPago);
    } catch (PagoRechazado e) {
      return volverAMascota(mascotaId, "?errorPago=" + e.getMessage());
    }
  }

  @RequestMapping(path = "/pago/exito", method = RequestMethod.GET)
  public ModelAndView pagoExito(
    @RequestParam(value = "payment_id", required = false) String paymentId,
    @RequestParam(value = "external_reference", required = false) String referencia,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);

    if (socio == null) {
      return new ModelAndView("redirect:/login");
    }
    try {
      servicioPago.confirmarPago(socio, paymentId, referencia);
      Long mascotaId = com.tallerwebi.dominio.ReferenciaPagoMascota.mascotaIdDe(referencia);
      if (mascotaId != null) {
        return volverAMascota(mascotaId, "?pagoOk=1");
      }
      return new ModelAndView("pago-exito");
    } catch (PagoRechazado e) {
      return new ModelAndView("pago-error", "mensaje", e.getMessage());
    }
  }

  @RequestMapping(path = "/pago/error", method = RequestMethod.GET)
  public ModelAndView pagoError() {
    return new ModelAndView("pago-error", "mensaje", "No se pudo completar el pago.");
  }

  @RequestMapping(path = "/pago/pendiente", method = RequestMethod.GET)
  public ModelAndView pagoPendiente() {
    return new ModelAndView("pago-pendiente");
  }

  private ModelAndView volverAMascota(Long mascotaId, String consulta) {
    return new ModelAndView("redirect:/mascotas/" + mascotaId + consulta);
  }
}
