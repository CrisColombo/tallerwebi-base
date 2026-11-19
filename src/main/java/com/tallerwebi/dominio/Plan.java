package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Plan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nombre;
  private String descripcion;
  private Double precio;

  // 1 = Básico, 2 = Premium. Un plan cubre los servicios de su nivel o menor.
  private Integer nivel;

  // Cuántas atenciones por mes cubre el plan. null = sin tope.
  private Integer topeMensual;

  // Lo que se paga por cada atención cubierta. null = sin copago.
  private Double copago;

  public boolean cubre(Servicio servicio) {
    return servicio.getNivelRequerido() <= nivel;
  }

  public boolean dentroDelTope(int numeroDeAtencionEnElMes) {
    return topeMensual == null || numeroDeAtencionEnElMes <= topeMensual;
  }

  public double copagoPorAtencion() {
    return copago == null ? 0 : copago;
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

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public Double getPrecio() {
    return precio;
  }

  public void setPrecio(Double precio) {
    this.precio = precio;
  }

  public Integer getNivel() {
    return nivel;
  }

  public void setNivel(Integer nivel) {
    this.nivel = nivel;
  }

  public Integer getTopeMensual() {
    return topeMensual;
  }

  public void setTopeMensual(Integer topeMensual) {
    this.topeMensual = topeMensual;
  }

  public Double getCopago() {
    return copago;
  }

  public void setCopago(Double copago) {
    this.copago = copago;
  }
}
