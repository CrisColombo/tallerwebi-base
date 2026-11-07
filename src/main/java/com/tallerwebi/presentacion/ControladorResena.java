package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioSocio;
import com.tallerwebi.dominio.ServicioTurno;
import com.tallerwebi.dominio.Socio;
import com.tallerwebi.dominio.Turno;
import com.tallerwebi.dominio.excepcion.ResenaInvalida;
import com.tallerwebi.dominio.excepcion.TurnoInvalido;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorResena extends ControladorConSocio {

  private final ServicioResena servicioResena;
  private final ServicioTurno servicioTurno;

  @Autowired
  public ControladorResena(
    ServicioSocio servicioSocio,
    ServicioResena servicioResena,
    ServicioTurno servicioTurno
  ) {
    super(servicioSocio);
    this.servicioResena = servicioResena;
    this.servicioTurno = servicioTurno;
  }

  @RequestMapping(path = "/turnos/{id}/resena", method = RequestMethod.POST)
  public ModelAndView crearResena(
    @PathVariable("id") Long turnoId,
    @RequestParam("puntuacion") int puntuacion,
    @RequestParam("comentario") String comentario,
    HttpSession sesion
  ) {
    Socio socio = socioLogueado(sesion);

    if (socio == null) {
      return new ModelAndView("redirect:/login");
    }

    try {
      servicioResena.crearResena(socio, turnoId, puntuacion, comentario);
      Long mascotaId = turnoIdMascota(socio, turnoId);

      if (mascotaId != null) {
        return new ModelAndView("redirect:/mascotas/" + mascotaId + "?resenaOk=1");
      }
      return new ModelAndView("redirect:/home");
    } catch (ResenaInvalida e) {
      Long mascotaId = turnoIdMascota(socio, turnoId);
      String err = mascotaId != null ? "?errorResena=" + e.getMessage() : "";
      if (mascotaId != null) {
        return new ModelAndView("redirect:/mascotas/" + mascotaId + err);
      }
      return new ModelAndView("redirect:/home");
    }
  }

  private Long turnoIdMascota(Socio socio, Long turnoId) {
    try {
      Turno turno = servicioTurno.buscarTurnoDe(socio, turnoId);
      return turno.getMascota().getId();
    } catch (TurnoInvalido e) {
      return null;
    }
  }
}
