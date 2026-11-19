package com.tallerwebi.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("PMD.TooManyFields")
@Entity
public class Turno {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate fecha;
  private LocalTime hora;

  @Enumerated(EnumType.STRING)
  private EstadoTurno estado = EstadoTurno.CONFIRMADO;

  private String observaciones;

  // Lo que anota la veterinaria al finalizar la consulta (historial clínico).
  @Column(length = 1000)
  private String observacionesProfesional;

  private boolean recordatorioEnviado;

  @ManyToOne
  private Mascota mascota;

  @ManyToOne
  private Veterinaria veterinaria;

  @ManyToOne
  private Servicio servicio;

  // Lo que habría costado la atención sin plan (lo carga la veterinaria al finalizar).
  private Double precioParticular;

  // Servicios extra que se sumaron al turno (radiografía, vacuna, etc.).
  @OneToMany(
    mappedBy = "turno",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.EAGER
  )
  private List<AtencionAdicional> adicionales = new ArrayList<>();

  public boolean estaConfirmado() {
    return estado == EstadoTurno.CONFIRMADO;
  }

  public void cancelar() {
    estado = EstadoTurno.CANCELADO;
  }

  public void finalizar() {
    estado = EstadoTurno.FINALIZADO;
  }

  /** Finaliza el turno y deja las observaciones del profesional (vacías = sin observaciones). */
  public void finalizar(String observacionesDelProfesional) {
    finalizar();
    observacionesProfesional =
      observacionesDelProfesional == null ? "" : observacionesDelProfesional.trim();
  }

  public String getObservacionesProfesional() {
    return observacionesProfesional;
  }

  public void setObservacionesProfesional(String observacionesProfesional) {
    this.observacionesProfesional = observacionesProfesional;
  }

  public boolean isRecordatorioEnviado() {
    return recordatorioEnviado;
  }

  public void marcarRecordatorioEnviado() {
    recordatorioEnviado = true;
  }

  public void reprogramar(LocalDate nuevaFecha, LocalTime nuevaHora) {
    setFecha(nuevaFecha);
    setHora(nuevaHora);
    recordatorioEnviado = false;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public LocalTime getHora() {
    return hora;
  }

  public void setHora(LocalTime hora) {
    this.hora = hora;
  }

  public EstadoTurno getEstado() {
    return estado;
  }

  public void setEstado(EstadoTurno estado) {
    this.estado = estado;
  }

  public String getObservaciones() {
    return observaciones;
  }

  public void setObservaciones(String observaciones) {
    this.observaciones = observaciones;
  }

  public Mascota getMascota() {
    return mascota;
  }

  public void setMascota(Mascota mascota) {
    this.mascota = mascota;
  }

  public Veterinaria getVeterinaria() {
    return veterinaria;
  }

  public void setVeterinaria(Veterinaria veterinaria) {
    this.veterinaria = veterinaria;
  }

  public Servicio getServicio() {
    return servicio;
  }

  public void setServicio(Servicio servicio) {
    this.servicio = servicio;
  }

  public boolean estaFinalizado() {
    return estado == EstadoTurno.FINALIZADO;
  }

  public void agregarAdicional(Servicio servicio, Double precio) {
    adicionales.add(new AtencionAdicional(this, servicio, precio));
  }

  public List<AtencionAdicional> getAdicionales() {
    return adicionales;
  }

  /** Precio del servicio del turno más el de todos los extras. */
  public double getTotalParticular() {
    double total = precioParticular == null ? 0 : precioParticular;
    for (AtencionAdicional adicional : adicionales) {
      total += adicional.getPrecio();
    }
    return total;
  }

  public Double getPrecioParticular() {
    return precioParticular;
  }

  public void setPrecioParticular(Double precioParticular) {
    this.precioParticular = precioParticular;
  }
}
