package com.tallerwebi.dominio;

public final class ReferenciaPagoMascota {

  private static final int PARTES_ESPERADAS = 4;

  private ReferenciaPagoMascota() {}

  public static String armar(Long mascotaId, Long planId) {
    return "mascota-" + mascotaId + "-plan-" + planId;
  }

  public static String armarUpgrade(Long mascotaId, Long planId) {
    return armar(mascotaId, planId) + "-upgrade";
  }

  public static boolean esUpgrade(String referencia) {
    return partesDe(referencia).length == PARTES_ESPERADAS + 1;
  }

  /*
   * Recibe el id de una mascota y el id de un plan
   * los concatena en un formato de texto ejemplo ->
   * "mascota-{mascotaId}-plan-{planId}"
   *
   * ejemplo si pasamos mascotaId=5 y planId = 2 devuelve -> "mascota-5-plan-2"
   */
  public static Long mascotaIdDe(String referencia) {
    String[] partes = partesDe(referencia);
    if (partes.length < PARTES_ESPERADAS) {
      return null;
    }
    return Long.parseLong(partes[1]);
  }

  public static Long planIdDe(String referencia) {
    String[] partes = partesDe(referencia);
    if (partes.length < PARTES_ESPERADAS) {
      return null;
    }
    return Long.parseLong(partes[3]);
  }

  private static String[] partesDe(String referencia) {
    if (referencia == null || !referencia.startsWith("mascota-")) {
      return new String[0];
    }
    String[] partes = referencia.split("-");
    boolean alta = partes.length == PARTES_ESPERADAS;
    boolean upgrade = partes.length == PARTES_ESPERADAS + 1 && "upgrade".equals(partes[4]);
    if (alta || upgrade) {
      return partes;
    }
    return new String[0];
  }
}
