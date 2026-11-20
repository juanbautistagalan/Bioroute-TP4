'use strict';

// ---------------------------------------------------------------------
// Configuración
// ---------------------------------------------------------------------
const DEPOSITO = { lat: -34.9205, lng: -57.9536, nombre: 'Depósito BioRoute (La Plata)' };
// Tiempo simulado que tarda el repartidor en recorrer la ruta desde que el pedido pasa a "En ruta"
const DURACION_RUTA_MS = 3 * 60 * 1000;
const PASOS = ['Pendiente', 'En preparacion', 'En ruta', 'Entregado'];
const ETIQUETAS = {
  'Pendiente': 'Pendiente',
  'En preparacion': 'En preparación',
  'En ruta': 'En ruta',
  'Entregado': 'Entregado',
  'Cancelado': 'Cancelado'
};
const CLAVE_EMAIL = 'bioroute.email';
const CLAVE_GEOCODIFICACION = 'bioroute.geo.';

const formatoPesos = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const formatoFecha = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

// ---------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------
const $ = (selector) => document.querySelector(selector);

function crear(etiqueta, clase, texto) {
  const el = document.createElement(etiqueta);
  if (clase) el.className = clase;
  if (texto !== undefined && texto !== null) el.textContent = texto;
  return el;
}

function etiquetaEstado(estado) {
  return ETIQUETAS[estado] || estado;
}

function claseEstado(estado) {
  return 'estado-' + String(estado).toLowerCase().replace(/\s+/g, '-');
}

function formatearFecha(timestamp) {
  return timestamp ? formatoFecha.format(new Date(timestamp)) : '';
}

function mostrarMensaje(el, texto, tipo) {
  el.textContent = texto;
  el.className = 'mensaje' + (tipo ? ' mensaje-' + tipo : '');
  el.hidden = !texto;
}

let temporizadorAviso = null;
function avisar(texto) {
  const aviso = $('#aviso');
  aviso.textContent = texto;
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 3000);
}

async function api(ruta, opciones = {}) {
  const config = { headers: { 'Accept': 'application/json' }, ...opciones };
  if (config.body && typeof config.body !== 'string') {
    config.body = JSON.stringify(config.body);
    config.headers['Content-Type'] = 'application/json';
  }
  let respuesta;
  try {
    respuesta = await fetch(ruta, config);
  } catch (e) {
    throw new Error('No se pudo conectar con el servidor.');
  }
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new Error((datos && datos.error) || 'Error ' + respuesta.status + ' en el servidor.');
  }
  return datos;
}

// ---------------------------------------------------------------------
// Navegación entre vistas (hash): #/catalogo, #/mis-pedidos, #/seguimiento/12
// ---------------------------------------------------------------------
function navegar() {
  const partes = location.hash.replace(/^#\/?/, '').split('/');
  const vista = ['catalogo', 'mis-pedidos', 'seguimiento'].includes(partes[0]) ? partes[0] : 'catalogo';

  document.querySelectorAll('.vista').forEach((s) => { s.hidden = s.id !== 'vista-' + vista; });
  document.querySelectorAll('.navegacion a').forEach((a) => {
    a.classList.toggle('activo', a.dataset.vista === vista);
  });

  if (vista !== 'seguimiento') detenerSeguimiento();

  if (vista === 'catalogo') {
    cargarProductos();
  } else if (vista === 'mis-pedidos') {
    const email = localStorage.getItem(CLAVE_EMAIL);
    const input = $('#form-mis-pedidos [name=email]');
    if (email && !input.value) {
      input.value = email;
      buscarPedidos(email);
    }
  } else if (vista === 'seguimiento') {
    const id = parseInt(partes[1], 10);
    if (id > 0) {
      iniciarSeguimiento(id);
    } else {
      detenerSeguimiento();
      $('#seguimiento-contenido').hidden = true;
      $('#seg-titulo').textContent = '';
      mostrarMensaje($('#mensaje-seguimiento'), 'Ingresá el número de pedido para seguirlo.');
    }
  }
}

// ---------------------------------------------------------------------
// Vista 1: catálogo y carrito
// ---------------------------------------------------------------------
let productos = [];
const carrito = new Map(); // idProducto -> cantidad

async function cargarProductos() {
  const contenedor = $('#lista-productos');
  try {
    productos = await api('/api/productos');
  } catch (e) {
    contenedor.replaceChildren(crear('p', 'mensaje mensaje-error', e.message));
    return;
  }
  // Ajustar el carrito si cambió el stock
  for (const [id, cantidad] of carrito) {
    const p = productos.find((x) => x.idProducto === id);
    if (!p) carrito.delete(id);
    else if (cantidad > p.stockActual) carrito.set(id, p.stockActual);
  }
  renderizarProductos();
  renderizarCarrito();
}

function renderizarProductos() {
  const contenedor = $('#lista-productos');
  if (productos.length === 0) {
    contenedor.replaceChildren(crear('p', 'vacio', 'No hay productos con stock disponible.'));
    return;
  }
  contenedor.replaceChildren(...productos.map((p) => {
    const tarjeta = crear('article', 'tarjeta-producto');
    tarjeta.append(
      crear('span', 'categoria', p.categoria || 'General'),
      crear('h3', null, p.nombre),
      crear('p', 'precio', formatoPesos.format(p.precioUnitario)),
      crear('p', 'stock', p.stockActual + ' disponibles')
    );
    const acciones = crear('div', 'acciones-producto');
    const cantidad = crear('input');
    cantidad.type = 'number';
    cantidad.min = '1';
    cantidad.max = String(p.stockActual);
    cantidad.value = '1';
    cantidad.setAttribute('aria-label', 'Cantidad de ' + p.nombre);
    const boton = crear('button', 'boton', 'Agregar');
    boton.type = 'button';
    boton.addEventListener('click', () => agregarAlCarrito(p, parseInt(cantidad.value, 10) || 1));
    acciones.append(cantidad, boton);
    tarjeta.append(acciones);
    return tarjeta;
  }));
}

function agregarAlCarrito(producto, cantidad) {
  const actual = carrito.get(producto.idProducto) || 0;
  const nueva = Math.min(actual + Math.max(1, cantidad), producto.stockActual);
  if (nueva === actual) {
    avisar('No hay más stock de ' + producto.nombre + '.');
    return;
  }
  carrito.set(producto.idProducto, nueva);
  avisar(producto.nombre + ' agregado al carrito.');
  renderizarCarrito();
}

function cambiarCantidad(idProducto, delta) {
  const producto = productos.find((p) => p.idProducto === idProducto);
  const nueva = (carrito.get(idProducto) || 0) + delta;
  if (nueva <= 0) carrito.delete(idProducto);
  else carrito.set(idProducto, Math.min(nueva, producto ? producto.stockActual : nueva));
  renderizarCarrito();
}

function renderizarCarrito() {
  const lista = $('#carrito-items');
  let total = 0;
  const items = [];
  for (const [id, cantidad] of carrito) {
    const p = productos.find((x) => x.idProducto === id);
    if (!p) continue;
    const subtotal = p.precioUnitario * cantidad;
    total += subtotal;

    const li = crear('li', 'carrito-item');
    const info = crear('div');
    info.append(crear('strong', null, p.nombre), crear('small', null, formatoPesos.format(subtotal)));
    const controles = crear('div', 'controles-cantidad');
    const menos = crear('button', 'boton-mini', '−');
    menos.type = 'button';
    menos.setAttribute('aria-label', 'Quitar uno');
    menos.addEventListener('click', () => cambiarCantidad(id, -1));
    const mas = crear('button', 'boton-mini', '+');
    mas.type = 'button';
    mas.setAttribute('aria-label', 'Agregar uno');
    mas.addEventListener('click', () => cambiarCantidad(id, 1));
    controles.append(menos, crear('span', null, String(cantidad)), mas);
    li.append(info, controles);
    items.push(li);
  }
  lista.replaceChildren(...items);
  $('#carrito-vacio').hidden = items.length > 0;
  $('#carrito-total').textContent = formatoPesos.format(total);
  $('#btn-confirmar').disabled = items.length === 0;
}

async function confirmarPedido(evento) {
  evento.preventDefault();
  const form = evento.target;
  const mensaje = $('#mensaje-pedido');
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }
  if (carrito.size === 0) {
    mostrarMensaje(mensaje, 'Agregá al menos un producto al carrito.', 'error');
    return;
  }

  const datos = Object.fromEntries(new FormData(form));
  const boton = $('#btn-confirmar');
  boton.disabled = true;
  mostrarMensaje(mensaje, 'Procesando pedido...');

  try {
    const { cliente, creado } = await api('/api/clientes', { method: 'POST', body: datos });
    const items = [...carrito].map(([idProducto, cantidad]) => ({ idProducto, cantidad }));
    const pedido = await api('/api/pedidos', {
      method: 'POST',
      body: { idCliente: cliente.idCliente, items }
    });

    localStorage.setItem(CLAVE_EMAIL, cliente.email);
    carrito.clear();
    form.reset();
    let texto = 'Pedido #' + pedido.idPedido + ' confirmado por ' + formatoPesos.format(pedido.total) + '.';
    if (!creado) texto += ' Ya estabas registrado: se entregará en ' + cliente.direccion + '.';
    mostrarMensaje(mensaje, texto, 'exito');
    const enlace = crear('a', 'enlace', ' Seguir pedido');
    enlace.href = '#/seguimiento/' + pedido.idPedido;
    mensaje.append(enlace);
    await cargarProductos();
  } catch (e) {
    mostrarMensaje(mensaje, e.message, 'error');
    renderizarCarrito();
  }
}

// ---------------------------------------------------------------------
// Vista 2: mis pedidos
// ---------------------------------------------------------------------
async function buscarPedidos(email) {
  const mensaje = $('#mensaje-mis-pedidos');
  const lista = $('#lista-pedidos');
  mostrarMensaje(mensaje, 'Buscando pedidos...');
  lista.replaceChildren();
  try {
    const pedidos = await api('/api/pedidos?email=' + encodeURIComponent(email));
    localStorage.setItem(CLAVE_EMAIL, email.trim().toLowerCase());
    if (pedidos.length === 0) {
      mostrarMensaje(mensaje, 'No encontramos pedidos para ' + email + '.');
      return;
    }
    mostrarMensaje(mensaje, '');
    lista.replaceChildren(...pedidos.map(renderizarResumenPedido));
  } catch (e) {
    mostrarMensaje(mensaje, e.message, 'error');
  }
}

function renderizarResumenPedido(p) {
  const tarjeta = crear('article', 'tarjeta-pedido');
  const cabecera = crear('div', 'tarjeta-pedido-cabecera');
  cabecera.append(
    crear('h3', null, 'Pedido #' + p.idPedido),
    crear('span', 'badge ' + claseEstado(p.estadoActual), etiquetaEstado(p.estadoActual))
  );
  const resumen = p.detalles.map((d) => d.cantidad + ' x ' + d.nombreProducto).join(', ');
  const pie = crear('div', 'tarjeta-pedido-pie');
  const enlace = crear('a', 'boton', 'Seguir pedido');
  enlace.href = '#/seguimiento/' + p.idPedido;
  pie.append(crear('strong', null, formatoPesos.format(p.total)), enlace);
  tarjeta.append(
    cabecera,
    crear('p', 'fecha', formatearFecha(p.timestampCreacion) + ' · ' + p.direccionCliente),
    crear('p', 'items', resumen),
    pie
  );
  return tarjeta;
}

// ---------------------------------------------------------------------
// Vista 3: seguimiento (SSE + mapa Leaflet)
// ---------------------------------------------------------------------
const seguimiento = {
  idPedido: null,
  fuente: null,          // EventSource
  estado: null,
  historial: [],
  desfaseReloj: 0,       // servidorAhora - Date.now()
  ruta: [],              // [[lat, lng], ...]
  distancias: [],        // distancia acumulada por punto
  animacion: null
};

let mapa = null;
let capaRuta = null;
let marcadorRepartidor = null;

function iconoDiv(clase, html) {
  return L.divIcon({ className: 'icono-mapa ' + clase, html, iconSize: [30, 30], iconAnchor: [15, 15] });
}

function asegurarMapa() {
  if (mapa) return;
  mapa = L.map('mapa').setView([DEPOSITO.lat, DEPOSITO.lng], 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; colaboradores de <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(mapa);
  capaRuta = L.layerGroup().addTo(mapa);
}

function detenerSeguimiento() {
  if (seguimiento.fuente) seguimiento.fuente.close();
  if (seguimiento.animacion) cancelAnimationFrame(seguimiento.animacion);
  seguimiento.fuente = null;
  seguimiento.animacion = null;
  seguimiento.idPedido = null;
}

async function iniciarSeguimiento(idPedido) {
  if (seguimiento.idPedido === idPedido && seguimiento.fuente) return;
  detenerSeguimiento();
  seguimiento.idPedido = idPedido;

  const mensaje = $('#mensaje-seguimiento');
  const contenido = $('#seguimiento-contenido');
  $('#seg-titulo').textContent = '· Pedido #' + idPedido;
  $('#form-seguimiento [name=idPedido]').value = idPedido;
  mostrarMensaje(mensaje, 'Cargando pedido...');

  let pedido;
  try {
    pedido = await api('/api/pedidos/' + idPedido);
  } catch (e) {
    contenido.hidden = true;
    mostrarMensaje(mensaje, e.message, 'error');
    return;
  }
  if (seguimiento.idPedido !== idPedido) return; // el usuario navegó a otro pedido

  mostrarMensaje(mensaje, '');
  contenido.hidden = false;
  $('#seg-cliente').textContent = pedido.nombreCliente;
  $('#seg-direccion').textContent = pedido.direccionCliente;
  $('#seg-total').textContent = formatoPesos.format(pedido.total);
  $('#seg-detalles').replaceChildren(...pedido.detalles.map((d) =>
    crear('li', null, d.cantidad + ' x ' + d.nombreProducto + ' — ' + formatoPesos.format(d.subtotal))));

  actualizarEstado(pedido.estadoActual, pedido.historial, null);

  asegurarMapa();
  mapa.invalidateSize();
  await prepararRuta(pedido.direccionCliente);
  if (seguimiento.idPedido !== idPedido) return;

  abrirEventos(idPedido);
  animar();
}

function abrirEventos(idPedido) {
  if (['Entregado', 'Cancelado'].includes(seguimiento.estado)) {
    marcarConexion('Seguimiento finalizado', false);
    return;
  }
  const fuente = new EventSource('/api/pedidos/' + idPedido + '/eventos');
  seguimiento.fuente = fuente;
  marcarConexion('Conectando...', false);

  fuente.addEventListener('estado', (evento) => {
    const datos = JSON.parse(evento.data);
    const anterior = seguimiento.estado;
    actualizarEstado(datos.estado, datos.historial, datos.servidorAhora);
    marcarConexion('En vivo · se actualiza cada 3 s', true);
    if (anterior && anterior !== datos.estado) {
      avisar('Tu pedido pasó a "' + etiquetaEstado(datos.estado) + '".');
    }
    if (datos.final) {
      // El servidor cierra la conexión; cerramos también aquí para que el navegador no reconecte
      fuente.close();
      seguimiento.fuente = null;
      marcarConexion('Seguimiento finalizado', false);
    }
  });
  fuente.addEventListener('error', (evento) => {
    if (evento.data) {
      mostrarMensaje($('#mensaje-seguimiento'), JSON.parse(evento.data).mensaje, 'error');
    }
    if (fuente.readyState === EventSource.CLOSED) {
      marcarConexion('Sin conexión', false);
    } else if (seguimiento.fuente === fuente) {
      marcarConexion('Reconectando...', false);
    }
  });
}

function marcarConexion(texto, enVivo) {
  const el = $('#seg-conexion');
  el.textContent = texto;
  el.classList.toggle('en-vivo', enVivo);
}

function actualizarEstado(estado, historial, servidorAhora) {
  seguimiento.estado = estado;
  seguimiento.historial = historial || [];
  if (servidorAhora) seguimiento.desfaseReloj = servidorAhora - Date.now();

  const badge = $('#seg-badge');
  badge.textContent = etiquetaEstado(estado);
  badge.className = 'badge ' + claseEstado(estado);
  renderizarLineaTiempo();
}

function ultimoCambio(estado) {
  for (let i = seguimiento.historial.length - 1; i >= 0; i--) {
    if (seguimiento.historial[i].estado === estado) return seguimiento.historial[i];
  }
  return null;
}

function renderizarLineaTiempo() {
  const estado = seguimiento.estado;
  const cancelado = estado === 'Cancelado';
  const pasos = cancelado
    ? PASOS.filter((p) => p !== 'Entregado' && (p === 'Pendiente' || ultimoCambio(p))).concat('Cancelado')
    : PASOS;
  const indiceActual = pasos.indexOf(estado);

  $('#linea-tiempo').replaceChildren(...pasos.map((paso, i) => {
    const cambio = ultimoCambio(paso);
    let clase = 'paso';
    if (i < indiceActual) clase += ' completado';
    if (i === indiceActual) clase += ' actual';
    if (paso === 'Cancelado') clase += ' cancelado';
    const li = crear('li', clase);
    li.append(crear('span', 'paso-punto'));
    const texto = crear('div', 'paso-texto');
    texto.append(crear('strong', null, etiquetaEstado(paso)));
    if (cambio) {
      texto.append(crear('small', null, formatearFecha(cambio.timestamp) +
        (cambio.observacion ? ' · ' + cambio.observacion : '')));
    } else if (i > indiceActual) {
      texto.append(crear('small', null, 'Pendiente de realizar'));
    }
    li.append(texto);
    return li;
  }));
}

// --- Ruta simulada ---------------------------------------------------

async function geocodificar(direccion) {
  const clave = CLAVE_GEOCODIFICACION + direccion.toLowerCase();
  const guardado = localStorage.getItem(clave);
  if (guardado) return JSON.parse(guardado);
  try {
    const url = 'https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=ar&q=' +
      encodeURIComponent(direccion);
    const respuesta = await fetch(url, { headers: { 'Accept-Language': 'es' } });
    const resultados = await respuesta.json();
    if (resultados.length > 0) {
      const punto = { lat: parseFloat(resultados[0].lat), lng: parseFloat(resultados[0].lon) };
      localStorage.setItem(clave, JSON.stringify(punto));
      return punto;
    }
  } catch (e) {
    // sin conexión al geocodificador: se usa un punto simulado
  }
  return puntoSimulado(direccion);
}

// Punto estable (mismo resultado para la misma dirección) a 2-5 km del depósito
function puntoSimulado(texto) {
  let hash = 0;
  for (const c of texto) hash = (hash * 31 + c.charCodeAt(0)) | 0;
  const angulo = (Math.abs(hash) % 360) * Math.PI / 180;
  const km = 2 + (Math.abs(hash >> 8) % 300) / 100;
  return {
    lat: DEPOSITO.lat + (km / 111) * Math.sin(angulo),
    lng: DEPOSITO.lng + (km / (111 * Math.cos(DEPOSITO.lat * Math.PI / 180))) * Math.cos(angulo)
  };
}

async function calcularRuta(origen, destino) {
  try {
    const url = 'https://router.project-osrm.org/route/v1/driving/' +
      origen.lng + ',' + origen.lat + ';' + destino.lng + ',' + destino.lat +
      '?overview=full&geometries=geojson';
    const respuesta = await fetch(url);
    const datos = await respuesta.json();
    if (datos.code === 'Ok' && datos.routes.length > 0) {
      return datos.routes[0].geometry.coordinates.map(([lng, lat]) => [lat, lng]);
    }
  } catch (e) {
    // sin servicio de ruteo: se genera una ruta aproximada
  }
  return rutaAproximada(origen, destino);
}

// Ruta en "escalera" (como calles en damero) entre origen y destino
function rutaAproximada(origen, destino) {
  const tramos = 6;
  const puntos = [[origen.lat, origen.lng]];
  for (let i = 1; i <= tramos; i++) {
    const t = i / tramos;
    const anterior = puntos[puntos.length - 1];
    const lat = origen.lat + (destino.lat - origen.lat) * t;
    const lng = origen.lng + (destino.lng - origen.lng) * t;
    puntos.push(i % 2 ? [lat, anterior[1]] : [anterior[0], lng]);
  }
  puntos.push([destino.lat, destino.lng]);
  return puntos;
}

async function prepararRuta(direccion) {
  capaRuta.clearLayers();
  marcadorRepartidor = null;
  $('#seg-progreso').textContent = 'Calculando ruta...';

  const destino = await geocodificar(direccion);
  const ruta = await calcularRuta(DEPOSITO, destino);
  seguimiento.ruta = ruta;
  seguimiento.distancias = [0];
  for (let i = 1; i < ruta.length; i++) {
    const d = L.latLng(ruta[i - 1]).distanceTo(L.latLng(ruta[i]));
    seguimiento.distancias.push(seguimiento.distancias[i - 1] + d);
  }

  const linea = L.polyline(ruta, { color: '#2e7d32', weight: 5, opacity: 0.8 }).addTo(capaRuta);
  L.marker([DEPOSITO.lat, DEPOSITO.lng], { icon: iconoDiv('icono-deposito', 'D'), title: DEPOSITO.nombre })
    .bindPopup(DEPOSITO.nombre).addTo(capaRuta);
  L.marker([destino.lat, destino.lng], { icon: iconoDiv('icono-destino', 'C'), title: direccion })
    .bindPopup('Entrega: ' + direccion.replace(/[<>&]/g, '')).addTo(capaRuta);
  marcadorRepartidor = L.marker(ruta[0], {
    icon: iconoDiv('icono-repartidor', svgCamion()),
    title: 'Repartidor',
    zIndexOffset: 1000
  }).addTo(capaRuta);
  mapa.fitBounds(linea.getBounds(), { padding: [40, 40] });
}

function svgCamion() {
  return '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" ' +
    'd="M3 5h11v9H3zM14 8h4l3 3v3h-7zM6.5 18a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm11 0a1.5 1.5 0 1 0 0-3 ' +
    '1.5 1.5 0 0 0 0 3z"/></svg>';
}

// Posición sobre la ruta para una fracción 0..1 del recorrido total (interpolación lineal)
function posicionEnRuta(fraccion) {
  const { ruta, distancias } = seguimiento;
  const total = distancias[distancias.length - 1];
  const objetivo = total * fraccion;
  for (let i = 1; i < ruta.length; i++) {
    if (distancias[i] >= objetivo) {
      const tramo = distancias[i] - distancias[i - 1];
      const t = tramo > 0 ? (objetivo - distancias[i - 1]) / tramo : 0;
      return [
        ruta[i - 1][0] + (ruta[i][0] - ruta[i - 1][0]) * t,
        ruta[i - 1][1] + (ruta[i][1] - ruta[i - 1][1]) * t
      ];
    }
  }
  return ruta[ruta.length - 1];
}

function fraccionRecorrida() {
  const estado = seguimiento.estado;
  if (estado === 'Entregado') return 1;
  if (estado !== 'En ruta') return 0;
  const cambio = ultimoCambio('En ruta');
  const inicio = cambio ? cambio.timestamp : Date.now() + seguimiento.desfaseReloj;
  const transcurrido = Date.now() + seguimiento.desfaseReloj - inicio;
  // Hasta que el repartidor confirme la entrega, el marcador se detiene cerca del destino
  return Math.min(Math.max(transcurrido / DURACION_RUTA_MS, 0), 0.98);
}

function animar() {
  if (marcadorRepartidor && seguimiento.ruta.length > 1) {
    const estado = seguimiento.estado;
    const fraccion = fraccionRecorrida();
    marcadorRepartidor.setLatLng(posicionEnRuta(fraccion));
    marcadorRepartidor.setOpacity(estado === 'Cancelado' ? 0.35 : 1);

    const progreso = $('#seg-progreso');
    if (estado === 'En ruta') {
      const restanteMin = Math.ceil((1 - fraccion) * DURACION_RUTA_MS / 60000);
      progreso.textContent = 'El repartidor está en camino · ' + Math.round(fraccion * 100) +
        '% del recorrido · llegada estimada en ' + restanteMin + ' min';
    } else if (estado === 'Entregado') {
      progreso.textContent = 'Pedido entregado.';
    } else if (estado === 'Cancelado') {
      progreso.textContent = 'Pedido cancelado.';
    } else {
      progreso.textContent = 'El pedido todavía está en el depósito (' + etiquetaEstado(estado) + ').';
    }
  }
  seguimiento.animacion = requestAnimationFrame(animar);
}

// ---------------------------------------------------------------------
// Inicio
// ---------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
  $('#form-cliente').addEventListener('submit', confirmarPedido);
  $('#form-mis-pedidos').addEventListener('submit', (e) => {
    e.preventDefault();
    buscarPedidos($('#form-mis-pedidos [name=email]').value);
  });
  $('#form-seguimiento').addEventListener('submit', (e) => {
    e.preventDefault();
    const id = parseInt($('#form-seguimiento [name=idPedido]').value, 10);
    if (id > 0) location.hash = '#/seguimiento/' + id;
  });
  window.addEventListener('hashchange', navegar);
  navegar();
});
