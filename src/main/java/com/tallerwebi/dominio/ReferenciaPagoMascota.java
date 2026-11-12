package com.tallerwebi.dominio;

public final class ReferenciaPagoMascota {

  private static final int PARTES_ESPERADAS = 4;

  private ReferenciaPagoMascota() {}

  public static String armar(Long mascotaId, Long planId) {
    return "mascota-" + mascotaId + "-plan-" + planId;
  }

  /*
   * Recibe el id de una mascota y el id de un plan
   * los concatena en un formato de texto ejemplo ->
   * "mascota-{mascotaId}-plan-{planId}"
   *
   * ejemplo si pasamos mascotaId=5 y planId = 2 devuelve -> "mascota-5-plan-2"
   */
  public static Long mascotaIdDe(String referencia) {
    if (referencia == null || !referencia.startsWith("mascota-")) {
      return null;
    }

    String[] partes = referencia.split("-");

    if (partes.length != PARTES_ESPERADAS) {
      return null;
    }
    return Long.parseLong(partes[1]);
  }

  public static Long planIdDe(String referencia) {
    if (referencia == null || !referencia.startsWith("mascota-")) {
      return null;
    }

    String[] partes = referencia.split("-");

    if (partes.length != PARTES_ESPERADAS) {
      return null;
    }

    return Long.parseLong(partes[3]);
  }
}
