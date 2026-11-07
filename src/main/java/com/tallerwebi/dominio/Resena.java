package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;

@Entity
public class Resena {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(unique = true) // evita 2 reseñas en el mimso turno
  private Turno turno;

  @ManyToOne(optional = false)
  private Veterinaria veterinaria;

  @ManyToOne(optional = false)
  private Socio socio;

  private int puntuacion; // 1 a 5

  @Column(length = 500)
  private String comentario;

  private LocalDateTime creadoEn = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Turno getTurno() {
    return turno;
  }

  public void setTurno(Turno turno) {
    this.turno = turno;
  }

  public Veterinaria getVeterinaria() {
    return veterinaria;
  }

  public void setVeterinaria(Veterinaria veterinaria) {
    this.veterinaria = veterinaria;
  }

  public Socio getSocio() {
    return socio;
  }

  public void setSocio(Socio socio) {
    this.socio = socio;
  }

  public int getPuntuacion() {
    return puntuacion;
  }

  public void setPuntuacion(int puntuacion) {
    this.puntuacion = puntuacion;
  }

  public String getComentario() {
    return comentario;
  }

  public void setComentario(String comentario) {
    this.comentario = comentario;
  }

  public LocalDateTime getCreadoEn() {
    return creadoEn;
  }

  public void setCreadoEn(LocalDateTime creadoEn) {
    this.creadoEn = creadoEn;
  }
}
