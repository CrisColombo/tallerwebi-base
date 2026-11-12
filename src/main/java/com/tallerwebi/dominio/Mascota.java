package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/* Entidad de la base de datos

  Mascota tiene un socio y un plan.
*/
@Entity
public class Mascota {

  /************** ATRIBUTOS **************/

  /* Identificador único de la mascota
     Tipo de dato: Long,
     Generación automática (tipo auto_increment)
  */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /* Nombre de la mascota */
  private String nombre;

  /* Especie de la mascota
    Tipo de dato: enum (PERRO, GATO, OTRO),
  */
  @Enumerated(EnumType.STRING)
  private Especie especie;

  /* Raza de la mascota */
  private String raza;

  /* Fecha de nacimiento de la mascota formato (YYYY-MM-DD) */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fechaNacimiento;

  /* Peso de la mascota */
  private Double peso;

  /************** RELACIONES **************/

  /* una mascota pertenece a un socio */
  @ManyToOne
  private Socio socio;

  /* una mascota tiene un plan */
  @ManyToOne
  private Plan plan;

  /* Hasta cuándo está paga la suscripción mensual */
  private LocalDate venceSuscripcion;

  /* Último pago de Mercado Pago que renovó la suscripción */
  private String ultimoPagoMercadoPago;

  /* una mascota puede cubrir un servicio */
  public boolean tieneCubierto(Servicio servicio) {
    return plan != null && suscripcionAlDia() && plan.cubre(servicio);
  }

  /************** GETTERS Y SETTERS **************/
  public Plan getPlan() {
    return plan;
  }

  public void setPlan(Plan plan) {
    this.plan = plan;
  }

  public LocalDate getVenceSuscripcion() {
    return venceSuscripcion;
  }

  public void setVenceSuscripcion(LocalDate venceSuscripcion) {
    this.venceSuscripcion = venceSuscripcion;
  }

  public String getUltimoPagoMercadoPago() {
    return ultimoPagoMercadoPago;
  }

  public void setUltimoPagoMercadoPago(String ultimoPagoMercadoPago) {
    this.ultimoPagoMercadoPago = ultimoPagoMercadoPago;
  }

  public boolean suscripcionAlDia() {
    return venceSuscripcion != null && !venceSuscripcion.isBefore(LocalDate.now());
  }

  public boolean sinPagoRegistrado() {
    return ultimoPagoMercadoPago == null || ultimoPagoMercadoPago.isBlank();
  }

  /** Suma un mes desde el vencimiento vigente, o desde hoy si ya venció. */
  public void renovarUnMes(String paymentId) {
    LocalDate desde = suscripcionAlDia() ? venceSuscripcion : LocalDate.now();
    venceSuscripcion = desde.plusMonths(1);
    ultimoPagoMercadoPago = paymentId;
  }

  public void limpiarSuscripcion() {
    venceSuscripcion = LocalDate.now().minusDays(1);
    ultimoPagoMercadoPago = "";
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public Especie getEspecie() {
    return especie;
  }

  public void setEspecie(Especie especie) {
    this.especie = especie;
  }

  public String getRaza() {
    return raza;
  }

  public void setRaza(String raza) {
    this.raza = raza;
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public Double getPeso() {
    return peso;
  }

  public void setPeso(Double peso) {
    this.peso = peso;
  }

  public Socio getSocio() {
    return socio;
  }

  public void setSocio(Socio socio) {
    this.socio = socio;
  }
}
