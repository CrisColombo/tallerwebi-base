/* global marked, DOMPurify */

// MediBot flotante: abre un panel de chat encima de cualquier pantalla del socio.

function mbHtmlSeguro(texto) {
  if (typeof marked === "undefined" || typeof DOMPurify === "undefined") {
    let div = document.createElement("div");
    div.textContent = texto;
    return div.innerHTML;
  }
  return DOMPurify.sanitize(marked.parse(texto, { breaks: true }));
}

function mbAgregar(contenedor, texto, deQuien) {
  let burbuja = document.createElement("div");
  burbuja.className = "mp-bot__msg mp-bot__msg--" + deQuien;
  if (deQuien === "bot") {
    burbuja.innerHTML = mbHtmlSeguro(texto);
  } else {
    burbuja.textContent = texto;
  }
  contenedor.appendChild(burbuja);
  contenedor.scrollTop = contenedor.scrollHeight;
  return burbuja;
}

function mbEnviar(url, contenedor, texto) {
  mbAgregar(contenedor, texto, "yo");
  let espera = mbAgregar(contenedor, "Escribiendo…", "bot");
  espera.classList.add("mp-bot__msg--espera");
  fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ mensaje: texto }),
    credentials: "same-origin",
  })
    .then(function (res) {
      if (!res.ok) throw new Error("Error " + res.status);
      return res.json();
    })
    .then(function (data) {
      espera.remove();
      mbAgregar(contenedor, data.respuesta, "bot");
    })
    .catch(function () {
      espera.remove();
      mbAgregar(contenedor, "No pude responder ahora. Probá de nuevo en un rato.", "bot");
    });
}

function inicializarMediBot() {
  let abrir = document.getElementById("mb-abrir");
  let panel = document.getElementById("mb-panel");
  if (!abrir || !panel) return;

  // En la página propia del chat el botón flotante sobra.
  if (document.getElementById("form-chat")) {
    abrir.hidden = true;
    return;
  }

  let cerrar = document.getElementById("mb-cerrar");
  let form = document.getElementById("mb-form");
  let input = document.getElementById("mb-input");
  let contenedor = document.getElementById("mb-mensajes");
  let url = form.dataset.endpoint;

  function mostrar(visible) {
    panel.hidden = !visible;
    abrir.setAttribute("aria-expanded", String(visible));
    abrir.classList.toggle("mp-fab--abierto", visible);
    if (visible) input.focus();
  }

  abrir.addEventListener("click", function () {
    mostrar(panel.hidden);
  });
  cerrar.addEventListener("click", function () {
    mostrar(false);
    abrir.focus();
  });
  document.addEventListener("keydown", function (evento) {
    if (evento.key === "Escape" && !panel.hidden) {
      mostrar(false);
      abrir.focus();
    }
  });

  form.addEventListener("submit", function (evento) {
    evento.preventDefault();
    let texto = input.value.trim();
    if (!texto) return;
    input.value = "";
    mbEnviar(url, contenedor, texto);
  });

  panel.querySelectorAll("[data-pregunta]").forEach(function (boton) {
    boton.addEventListener("click", function () {
      mbEnviar(url, contenedor, boton.dataset.pregunta);
    });
  });
}

document.addEventListener("DOMContentLoaded", inicializarMediBot);
