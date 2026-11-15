package com.tallerwebi.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Qué tiene que pagar el socio para pasar al plan elegido, con la fecha de hoy fija. */
public final class CotizacionSuscripcion {

  public enum Tipo {
    BLOQUEADO,
    UPGRADE,
    RENOVACION,
    ALTA,
    BAJA,
  }

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String YA_ACTIVO = "Ya tenés este plan activo";
  private static final String SIN_TIEMPO = "No queda tiempo del mes para cobrar la diferencia";
  private static final String SIN_CARGO = "Sin cargo. El plan baja y el vencimiento no cambia";
  private static final double IMPORTE_MINIMO = 0.01;

  private final Plan plan;
  private final Tipo tipo;
  private final double importe;
  private final String detalle;
  private final String textoBoton;

  private CotizacionSuscripcion(
    Plan plan,
    Tipo tipo,
    double importe,
    String detalle,
    String textoBoton
  ) {
    this.plan = plan;
    this.tipo = tipo;
    this.importe = importe;
    this.detalle = detalle;
    this.textoBoton = textoBoton;
  }

  public static CotizacionSuscripcion cotizar(Mascota mascota, Plan destino, LocalDate hoy) {
    if (destino == null || destino.getPrecio() == null) {
      return bloqueado(destino, "Plan inválido");
    }
    if (alDia(mascota, hoy)) {
      return cotizarConSuscripcionActiva(mascota, destino, hoy);
    }
    return cotizarAltaORenovacion(mascota, destino);
  }

  public Plan getPlan() {
    return plan;
  }

  public Tipo getTipo() {
    return tipo;
  }

  public double getImporte() {
    return importe;
  }

  public String getDetalle() {
    return detalle;
  }

  public String getTextoBoton() {
    return textoBoton;
  }

  public boolean permitePagar() {
    return tipo == Tipo.UPGRADE || tipo == Tipo.RENOVACION || tipo == Tipo.ALTA;
  }

  public boolean esUpgrade() {
    return tipo == Tipo.UPGRADE;
  }

  public boolean esBaja() {
    return tipo == Tipo.BAJA;
  }

  private static CotizacionSuscripcion cotizarConSuscripcionActiva(
    Mascota mascota,
    Plan destino,
    LocalDate hoy
  ) {
    Plan actual = mascota.getPlan();
    if (esElMismoPlan(actual, destino)) {
      return bloqueado(destino, YA_ACTIVO);
    }
    if (esUpgrade(actual, destino)) {
      return cotizarUpgrade(actual, destino, mascota.getVenceSuscripcion(), hoy);
    }
    if (esDowngrade(actual, destino)) {
      return new CotizacionSuscripcion(destino, Tipo.BAJA, 0, SIN_CARGO, "Bajar de plan");
    }
    return bloqueado(destino, YA_ACTIVO);
  }

  private static CotizacionSuscripcion cotizarUpgrade(
    Plan actual,
    Plan destino,
    LocalDate vence,
    LocalDate hoy
  ) {
    LocalDate inicio = vence.minusMonths(1);
    long diasPeriodo = Math.max(ChronoUnit.DAYS.between(inicio, vence), 1);
    long diasQueFaltan = ChronoUnit.DAYS.between(hoy, vence);
    double diferencia = destino.getPrecio() - actual.getPrecio();
    double bruto = (diferencia * diasQueFaltan) / diasPeriodo;
    double proporcional = BigDecimal.valueOf(bruto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    if (proporcional < IMPORTE_MINIMO) {
      return bloqueado(destino, SIN_TIEMPO);
    }
    String dias = diasQueFaltan == 1 ? "día" : "días";
    String detalle =
      "Pagás " +
      pesos(proporcional) +
      " por los " +
      diasQueFaltan +
      " " +
      dias +
      " que faltan hasta el " +
      vence.format(FECHA);
    return new CotizacionSuscripcion(
      destino,
      Tipo.UPGRADE,
      proporcional,
      detalle,
      "Pagar diferencia"
    );
  }

  private static CotizacionSuscripcion cotizarAltaORenovacion(Mascota mascota, Plan destino) {
    boolean sinPago = mascota == null || mascota.sinPagoRegistrado();
    if (sinPago) {
      return new CotizacionSuscripcion(
        destino,
        Tipo.ALTA,
        destino.getPrecio(),
        "Pagás " + pesos(destino.getPrecio()) + " por un mes",
        "Pagar con Mercado Pago"
      );
    }
    return new CotizacionSuscripcion(
      destino,
      Tipo.RENOVACION,
      destino.getPrecio(),
      "Renovás un mes por " + pesos(destino.getPrecio()),
      "Renovar suscripción"
    );
  }

  private static boolean alDia(Mascota mascota, LocalDate hoy) {
    if (mascota == null || mascota.getVenceSuscripcion() == null || hoy == null) {
      return false;
    }
    return !mascota.getVenceSuscripcion().isBefore(hoy);
  }

  private static boolean esElMismoPlan(Plan actual, Plan destino) {
    return actual != null && actual.getId() != null && actual.getId().equals(destino.getId());
  }

  private static boolean esUpgrade(Plan actual, Plan destino) {
    Integer nivelActual = nivelDe(actual);
    Integer nivelNuevo = nivelDe(destino);
    return nivelActual != null && nivelNuevo != null && nivelNuevo > nivelActual;
  }

  private static boolean esDowngrade(Plan actual, Plan destino) {
    Integer nivelActual = nivelDe(actual);
    Integer nivelNuevo = nivelDe(destino);
    return nivelActual != null && nivelNuevo != null && nivelNuevo < nivelActual;
  }

  private static Integer nivelDe(Plan plan) {
    return plan == null ? null : plan.getNivel();
  }

  private static CotizacionSuscripcion bloqueado(Plan destino, String detalle) {
    return new CotizacionSuscripcion(destino, Tipo.BLOQUEADO, 0, detalle, detalle);
  }

  private static String pesos(double importe) {
    boolean entero = Math.abs(importe - Math.rint(importe)) < 0.001;
    DecimalFormat formato = new DecimalFormat(
      entero ? "#,##0" : "#,##0.00",
      new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))
    );
    return "$" + formato.format(importe);
  }
}
