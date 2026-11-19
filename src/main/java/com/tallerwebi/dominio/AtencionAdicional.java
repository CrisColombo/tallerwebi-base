package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Servicio extra que la veterinaria suma a un turno mientras atiende al paciente. */
@Entity
public class AtencionAdicional {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  private Turno turno;

  @ManyToOne
  private Servicio servicio;

  // Precio que tenía el servicio al momento de la atención (no cambia si el catálogo cambia).
  private Double precio;

  public AtencionAdicional() {}

  public AtencionAdicional(Turno turno, Servicio servicio, Double precio) {
    this.turno = turno;
    this.servicio = servicio;
    this.precio = precio;
  }

  public Long getId() {
    return id;
  }

  public Turno getTurno() {
    return turno;
  }

  public Servicio getServicio() {
    return servicio;
  }

  public Double getPrecio() {
    return precio;
  }
}
