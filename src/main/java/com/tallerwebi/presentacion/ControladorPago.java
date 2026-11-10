package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ReferenciaPagoMascota;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.excepcion.PagoRechazado;
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
      HttpSession sesion) {
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
      @RequestParam(value = "collection_id", required = false) String collectionId,
      @RequestParam(value = "external_reference", required = false) String referencia,
      HttpSession sesion) {
    Socio socio = socioLogueado(sesion);

    if (socio == null) {
      return new ModelAndView("redirect:/login");
    }
    try {
      servicioPago.confirmarPago(socio, idDePago(paymentId, collectionId), referencia);
      return vistaResultado("pago-exito", referencia, null);
    } catch (PagoRechazado e) {
      return vistaResultado("pago-error", referencia, e.getMessage());
    }
  }

  @RequestMapping(path = "/pago/error", method = RequestMethod.GET)
  public ModelAndView pagoError(
      @RequestParam(value = "external_reference", required = false) String referencia) {
    return vistaResultado("pago-error", referencia, "No se pudo completar el pago.");
  }

  @RequestMapping(path = "/pago/pendiente", method = RequestMethod.GET)
  public ModelAndView pagoPendiente(
      @RequestParam(value = "external_reference", required = false) String referencia) {
    return vistaResultado("pago-pendiente", referencia, null);
  }

  private static String idDePago(String paymentId, String collectionId) {
    if (idUtil(paymentId))
      return paymentId;
    if (idUtil(collectionId))
      return collectionId;
    return null;
  }

  private static boolean idUtil(String id) {
    return id != null && !id.isBlank() && !"null".equals(id);
  }

  private ModelAndView vistaResultado(String vista, String referencia, String mensaje) {
    Map<String, Object> modelo = new ModelMap();
    Long mascotaId = ReferenciaPagoMascota.mascotaIdDe(referencia);
    if (mascotaId != null)
      modelo.put("mascotaId", mascotaId);
    if (mensaje != null)
      modelo.put("mensaje", mensaje);
    return new ModelAndView(vista, modelo);
  }

  private ModelAndView volverAMascota(Long mascotaId, String consulta) {
    return new ModelAndView("redirect:/mascotas/" + mascotaId + consulta);
  }
}
