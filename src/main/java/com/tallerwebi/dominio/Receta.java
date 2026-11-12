package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Receta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  private Turno turno;

  private String medicamento;
  private String indicaciones;

  @Enumerated(EnumType.STRING)
  private EstadoReceta estado = EstadoReceta.PENDIENTE;

  private LocalDate fechaCompra;

  public Receta() {}

  public Receta(Turno turno, String medicamento, String indicaciones) {
    this.turno = turno;
    this.medicamento = medicamento;
    this.indicaciones = indicaciones;
  }

  public boolean estaPendiente() {
    return estado == EstadoReceta.PENDIENTE;
  }

  public void marcarComprada(LocalDate fecha) {
    estado = EstadoReceta.COMPRADA;
    fechaCompra = fecha;
  }

  public Long getId() {
    return id;
  }

  public Turno getTurno() {
    return turno;
  }

  public String getMedicamento() {
    return medicamento;
  }

  public String getIndicaciones() {
    return indicaciones;
  }

  public EstadoReceta getEstado() {
    return estado;
  }

  public LocalDate getFechaCompra() {
    return fechaCompra;
  }
}
