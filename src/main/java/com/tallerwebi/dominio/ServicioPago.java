package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.PagoRechazado;

public interface ServicioPago {
  /* Devuelve la URL de Mercado Pago (La integracion que hicimos con sandbox) para que nuestro socio pague */
  String iniciarPagoPlan(Socio socio, Long mascotaId, Long planId) throws PagoRechazado;

  void confirmarPago(Socio socio, String paymentId, String referenciaExterna) throws PagoRechazado;
}
