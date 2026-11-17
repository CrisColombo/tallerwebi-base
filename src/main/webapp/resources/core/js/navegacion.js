// Marca en el menú y en la barra inferior la sección en la que está el usuario.

function rutaDe(enlace) {
  return new URL(enlace.href, window.location.origin).pathname;
}

// Devuelve el enlace cuya ruta es la más larga que coincide con la página actual.
function enlaceActivo(enlaces, rutaActual) {
  let elegido = null;
  let largo = 0;
  enlaces.forEach(function (enlace) {
    let ruta = rutaDe(enlace);
    let coincide = rutaActual === ruta || rutaActual.startsWith(ruta + "/");
    if (coincide && ruta.length > largo) {
      elegido = enlace;
      largo = ruta.length;
    }
  });
  return elegido;
}

function marcar(selector) {
  let enlaces = Array.from(document.querySelectorAll(selector)).filter(function (enlace) {
    return enlace.getAttribute("href") && enlace.getAttribute("href") !== "#";
  });
  let activo = enlaceActivo(enlaces, window.location.pathname);
  if (!activo) return;
  activo.classList.add("active");
  activo.setAttribute("aria-current", "page");
  let desplegable = activo.closest(".dropdown");
  if (desplegable) {
    desplegable.querySelector(".dropdown-toggle").classList.add("active");
  }
}

document.addEventListener("DOMContentLoaded", function () {
  marcar(".mp-header .nav-link, .mp-header .dropdown-item");
  marcar(".mp-tabbar a");
});
