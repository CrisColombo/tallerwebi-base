package com.tallerwebi.infraestructura;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClienteMercadoPago {

  private static final String ESTADO_APROBADO = "approved";

  public ClienteMercadoPago() {
    MercadoPagoConfig.setAccessToken(tokenObligatorio());
  }

  public String crearPreferencia(String tituloItem, double precio, String referenciaExterna) {
    String base = baseUrlObligatoria();
    PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest
      .builder()
      .success(base + "/pago/exito")
      .failure(base + "/pago/error")
      .pending(base + "/pago/pendiente")
      .build();
    PreferenceRequest preferencia = PreferenceRequest
      .builder()
      .items(
        List.of(
          PreferenceItemRequest
            .builder()
            .title(tituloItem)
            .quantity(1)
            .unitPrice(BigDecimal.valueOf(precio))
            .currencyId("ARS")
            .build()
        )
      )
      .backUrls(backUrls)
      .externalReference(referenciaExterna)
      // .autoReturn("approved")
      .build();
    PreferenceClient client = new PreferenceClient();
    try {
      Preference preference = client.create(preferencia);
      String url = preference.getSandboxInitPoint();
      if (url == null || url.isBlank()) {
        throw new IllegalStateException("Mercado Pago no devolvió sandbox_init_point");
      }
      return url;
    } catch (MPApiException e) {
      throw new IllegalStateException(
        "Error MP al crear preferencia: " + e.getApiResponse().getStatusCode(),
        e
      );
    } catch (MPException e) {
      throw new IllegalStateException("No se pudo crear la preferencia de pago", e);
    }
  }

  public boolean estaAprobado(String paymentId) {
    Payment payment = buscarPago(paymentId);
    return payment != null && ESTADO_APROBADO.equals(payment.getStatus());
  }

  private Payment buscarPago(String paymentId) {
    try {
      return new PaymentClient().get(Long.parseLong(paymentId.trim()));
    } catch (MPApiException | MPException | NumberFormatException e) {
      return null;
    }
  }

  private static String tokenObligatorio() {
    String token = System.getenv("MP_ACCESS_TOKEN");
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("Definí MP_ACCESS_TOKEN en el entorno");
    }
    return token;
  }

  private static String baseUrlObligatoria() {
    String base = System.getenv("APP_BASE_URL");
    if (base == null || base.isBlank()) {
      throw new IllegalStateException("Definí APP_BASE_URL en el entorno");
    }
    return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
  }
}
