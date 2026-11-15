package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.PagoRechazado;
import com.tallerwebi.infraestructura.ClienteMercadoPago;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioPagoImpl implements ServicioPago {

  private final ClienteMercadoPago clienteMercadoPago;
  private final ServicioMascota servicioMascota;
  private final RepositorioPlan repositorioPlan;

  @Autowired
  public ServicioPagoImpl(
    ClienteMercadoPago clienteMercadoPago,
    ServicioMascota servicioMascota,
    RepositorioPlan repositorioPlan
  ) {
    this.clienteMercadoPago = clienteMercadoPago;
    this.servicioMascota = servicioMascota;
    this.repositorioPlan = repositorioPlan;
  }

  @Override
  public String iniciarPagoPlan(Socio socio, Long mascotaId, Long planId) throws PagoRechazado {
    Mascota mascota = servicioMascota.buscarPorIdParaSocio(socio, mascotaId);
    if (mascota == null) {
      throw new PagoRechazado("No encontramos esa mascota");
    }
    Plan plan = repositorioPlan.buscarPorId(planId);
    if (plan == null || plan.getPrecio() == null) {
      throw new PagoRechazado("Plan inválido");
    }
    CotizacionSuscripcion cotizacion = CotizacionSuscripcion.cotizar(
      mascota,
      plan,
      LocalDate.now()
    );
    if (!cotizacion.permitePagar()) {
      throw new PagoRechazado(mensajeSiNoSePuedePagar(cotizacion));
    }
    String referencia = cotizacion.esUpgrade()
      ? ReferenciaPagoMascota.armarUpgrade(mascotaId, planId)
      : ReferenciaPagoMascota.armar(mascotaId, planId);
    String titulo = plan.getNombre() + " - " + mascota.getNombre();
    return clienteMercadoPago.crearPreferencia(titulo, cotizacion.getImporte(), referencia);
  }

  private static String mensajeSiNoSePuedePagar(CotizacionSuscripcion cotizacion) {
    if (cotizacion.esBaja()) {
      return "Para bajar de plan no hace falta pagar";
    }
    return cotizacion.getDetalle();
  }

  @Override
  public void confirmarPago(Socio socio, String paymentId, String referenciaExterna)
    throws PagoRechazado {
    exigirPagoAprobado(paymentId);
    Long mascotaId = ReferenciaPagoMascota.mascotaIdDe(referenciaExterna);
    Long planId = ReferenciaPagoMascota.planIdDe(referenciaExterna);
    exigirReferencia(mascotaId, planId);
    activarSuscripcion(
      socio,
      mascotaId,
      planId,
      paymentId,
      ReferenciaPagoMascota.esUpgrade(referenciaExterna)
    );
  }

  private void exigirPagoAprobado(String paymentId) throws PagoRechazado {
    boolean faltaId = paymentId == null || paymentId.isBlank();
    if (faltaId || !clienteMercadoPago.estaAprobado(paymentId)) {
      throw new PagoRechazado("El pago no fue aprobado");
    }
  }

  private void exigirReferencia(Long mascotaId, Long planId) throws PagoRechazado {
    if (mascotaId == null || planId == null) {
      throw new PagoRechazado("Referencia de pago inválida");
    }
  }

  private void activarSuscripcion(
    Socio socio,
    Long mascotaId,
    Long planId,
    String paymentId,
    boolean upgrade
  ) throws PagoRechazado {
    servicioMascota.cambiarPlan(socio, mascotaId, planId);
    Mascota mascota = servicioMascota.buscarPorId(mascotaId);
    if (mascota == null) {
      throw new PagoRechazado("No encontramos esa mascota");
    }
    if (upgrade || paymentId.equals(mascota.getUltimoPagoMercadoPago())) {
      return;
    }
    mascota.renovarUnMes(paymentId);
  }
}
