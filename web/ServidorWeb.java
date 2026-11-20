package bioroute.web;

import bioroute.dao.ClienteDAO;
import bioroute.dao.EstadoPedidoDAO;
import bioroute.dao.PedidoDAO;
import bioroute.dao.ProductoDAO;
import bioroute.dao.UsuarioDAO;
import bioroute.exception.DatosException;
import bioroute.exception.ValidacionException;
import bioroute.modelo.Cliente;
import bioroute.modelo.DetallePedido;
import bioroute.modelo.EstadoPedido;
import bioroute.modelo.Pedido;
import bioroute.modelo.Producto;
import bioroute.util.AuditoriaArchivo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.sse.SseClient;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Servidor web del portal de clientes de BioRoute (Javalin).
 * Sirve el frontend estatico de web/public y expone una API REST + SSE
 * reutilizando los DAO existentes.
 *
 * Variables de entorno opcionales:
 *   BIOROUTE_WEB_PUERTO   puerto HTTP (por defecto 8080)
 *   BIOROUTE_WEB_USUARIO  usuario que registra los pedidos web (por defecto "portal-web")
 */
public class ServidorWeb {

    private static final int PUERTO_DEFECTO = 8080;
    private static final String USUARIO_PORTAL_DEFECTO = "portal-web";
    private static final int INTERVALO_SSE_SEGUNDOS = 3;
    private static final int CANTIDAD_MAXIMA = 10_000;
    private static final Set<String> ESTADOS_FINALES = Set.of("Entregado", "Cancelado");
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Path CARPETA_PUBLICA = Path.of("web", "public");

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final EstadoPedidoDAO estadoPedidoDAO = new EstadoPedidoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private final String nombreUsuarioPortal;
    private volatile int idUsuarioPortal = -1;

    private final ScheduledExecutorService planificadorSse = Executors.newScheduledThreadPool(4, r -> {
        Thread hilo = new Thread(r, "bioroute-sse");
        hilo.setDaemon(true);
        return hilo;
    });

    // Cuerpos JSON esperados por la API
    public record SolicitudCliente(String nombre, String email, String telefono, String direccion) { }
    public record ItemPedido(Integer idProducto, Integer cantidad) { }
    public record SolicitudPedido(Integer idCliente, List<ItemPedido> items) { }

    public ServidorWeb(String nombreUsuarioPortal) {
        this.nombreUsuarioPortal = nombreUsuarioPortal;
    }

    public static void main(String[] args) {
        int puerto = Integer.parseInt(leerEntorno("BIOROUTE_WEB_PUERTO", String.valueOf(PUERTO_DEFECTO)));
        String usuario = leerEntorno("BIOROUTE_WEB_USUARIO", USUARIO_PORTAL_DEFECTO);
        new ServidorWeb(usuario).crearApp().start(puerto);
        System.out.println("Portal web de BioRoute disponible en http://localhost:" + puerto);
    }

    public Javalin crearApp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.staticFiles.add(estaticos -> {
                estaticos.hostedPath = "/";
                if (Files.isDirectory(CARPETA_PUBLICA)) {
                    // Ejecutando desde la raiz del repo: se sirven los archivos tal cual (sin recompilar)
                    estaticos.directory = CARPETA_PUBLICA.toAbsolutePath().toString();
                    estaticos.location = Location.EXTERNAL;
                } else {
                    // Ejecutando desde el jar: los archivos se empaquetan en /public
                    estaticos.directory = "/public";
                    estaticos.location = Location.CLASSPATH;
                }
            });
        });

        app.get("/api/productos", this::listarProductos);
        app.post("/api/clientes", this::crearOBuscarCliente);
        app.post("/api/pedidos", this::crearPedido);
        app.get("/api/pedidos", this::listarPedidosPorEmail);
        app.get("/api/pedidos/{id}", this::detallePedido);
        app.sse("/api/pedidos/{id}/eventos", this::seguirPedido);

        app.exception(ValidacionException.class, (e, ctx) -> error(ctx, HttpStatus.BAD_REQUEST, e.getMessage()));
        app.exception(DatosException.class, (e, ctx) -> {
            System.err.println("Error de datos: " + e.getMessage()
                + (e.getCause() != null ? " - " + e.getCause().getMessage() : ""));
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        });

        app.events(eventos -> eventos.serverStopping(planificadorSse::shutdownNow));
        return app;
    }

    // ---------------------------------------------------------------------
    // Endpoints
    // ---------------------------------------------------------------------

    private void listarProductos(Context ctx) throws DatosException {
        List<Map<String, Object>> respuesta = new ArrayList<>();
        for (Producto p : productoDAO.listar()) {
            if (p.getStockActual() > 0) {
                respuesta.add(mapaProducto(p));
            }
        }
        ctx.json(respuesta);
    }

    private void crearOBuscarCliente(Context ctx) throws DatosException, ValidacionException {
        SolicitudCliente solicitud = leerCuerpo(ctx, SolicitudCliente.class);
        String email = normalizarEmail(solicitud.email());

        Cliente existente = clienteDAO.buscarPorEmail(email);
        if (existente != null) {
            ctx.json(Map.of("cliente", mapaCliente(existente), "creado", false));
            return;
        }

        String nombre = textoObligatorio(solicitud.nombre(), "nombre", 100);
        String direccion = textoObligatorio(solicitud.direccion(), "direccion", 200);
        String telefono = textoOpcional(solicitud.telefono(), "telefono", 20);

        Cliente nuevo = new Cliente(nombre, telefono, direccion, email);
        int id = clienteDAO.crear(nuevo);
        AuditoriaArchivo.registrar("Portal web - cliente registrado: " + nombre + " (" + email + ")");

        ctx.status(HttpStatus.CREATED).json(Map.of(
            "cliente", mapaCliente(new Cliente(id, nombre, telefono, direccion, email)),
            "creado", true));
    }

    private void crearPedido(Context ctx) throws DatosException, ValidacionException {
        SolicitudPedido solicitud = leerCuerpo(ctx, SolicitudPedido.class);
        if (solicitud.idCliente() == null || solicitud.idCliente() <= 0) {
            throw new ValidacionException("Falta el idCliente.");
        }
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new ValidacionException("El pedido debe tener al menos un producto.");
        }
        if (clienteDAO.buscarPorId(solicitud.idCliente()) == null) {
            error(ctx, HttpStatus.NOT_FOUND, "No existe el cliente " + solicitud.idCliente() + ".");
            return;
        }

        // Agrupar cantidades por producto (por si el mismo producto viene repetido)
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedido item : solicitud.items()) {
            if (item == null || item.idProducto() == null || item.cantidad() == null
                    || item.cantidad() <= 0 || item.cantidad() > CANTIDAD_MAXIMA) {
                throw new ValidacionException("Cada item debe tener idProducto y una cantidad entre 1 y "
                    + CANTIDAD_MAXIMA + ".");
            }
            int total = cantidades.merge(item.idProducto(), item.cantidad(), Integer::sum);
            if (total > CANTIDAD_MAXIMA) {
                throw new ValidacionException("La cantidad por producto no puede superar " + CANTIDAD_MAXIMA + ".");
            }
        }

        Map<Integer, Producto> catalogo = new LinkedHashMap<>();
        for (Producto p : productoDAO.listar()) catalogo.put(p.getIdProducto(), p);

        // El precio se toma siempre de la base, nunca del cliente
        Pedido pedido = new Pedido(solicitud.idCliente(), obtenerIdUsuarioPortal());
        for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {
            Producto producto = catalogo.get(entrada.getKey());
            if (producto == null) {
                throw new ValidacionException("No existe el producto " + entrada.getKey() + ".");
            }
            if (producto.getStockActual() < entrada.getValue()) {
                error(ctx, HttpStatus.CONFLICT, "Stock insuficiente para '" + producto.getNombre()
                    + "' (disponible: " + producto.getStockActual() + ").");
                return;
            }
            pedido.agregarDetalle(new DetallePedido(
                producto.getIdProducto(), producto.getNombre(), entrada.getValue(), producto.getPrecioUnitario()));
        }

        int idPedido = pedidoDAO.crear(pedido);
        AuditoriaArchivo.registrar("Portal web - pedido creado: " + idPedido);

        ctx.status(HttpStatus.CREATED).json(Map.of(
            "idPedido", idPedido,
            "estado", "Pendiente",
            "total", pedido.calcularTotal()));
    }

    private void listarPedidosPorEmail(Context ctx) throws DatosException, ValidacionException {
        String email = normalizarEmail(ctx.queryParam("email"));
        List<Map<String, Object>> respuesta = new ArrayList<>();
        for (Pedido p : pedidoDAO.listarPorEmailCliente(email)) {
            respuesta.add(mapaPedido(p));
        }
        ctx.json(respuesta);
    }

    private void detallePedido(Context ctx) throws DatosException, ValidacionException {
        int idPedido = leerIdPedido(ctx);
        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            error(ctx, HttpStatus.NOT_FOUND, "No existe el pedido " + idPedido + ".");
            return;
        }
        Map<String, Object> respuesta = mapaPedido(pedido);
        respuesta.put("historial", mapaHistorial(estadoPedidoDAO.listarPorPedido(idPedido)));
        ctx.json(respuesta);
    }

    /**
     * SSE: emite el evento "estado" cada 3 segundos hasta que el pedido
     * llega a un estado final (Entregado o Cancelado) y luego cierra la conexion.
     */
    private void seguirPedido(SseClient cliente) {
        int idPedido;
        try {
            idPedido = Integer.parseInt(cliente.ctx().pathParam("id"));
        } catch (NumberFormatException e) {
            cliente.sendEvent("error", Map.of("mensaje", "Identificador de pedido invalido."));
            cliente.close();
            return;
        }
        cliente.keepAlive();
        SeguimientoSse seguimiento = new SeguimientoSse(cliente, idPedido);
        cliente.onClose(seguimiento::detener);
        seguimiento.iniciar();
    }

    private final class SeguimientoSse implements Runnable {
        private final SseClient cliente;
        private final int idPedido;
        private volatile ScheduledFuture<?> tarea;
        private volatile boolean terminado;

        SeguimientoSse(SseClient cliente, int idPedido) {
            this.cliente = cliente;
            this.idPedido = idPedido;
        }

        void iniciar() {
            tarea = planificadorSse.scheduleAtFixedRate(this, 0, INTERVALO_SSE_SEGUNDOS, TimeUnit.SECONDS);
            if (terminado) tarea.cancel(false);
        }

        void detener() {
            terminado = true;
            if (tarea != null) tarea.cancel(false);
        }

        @Override
        public void run() {
            if (terminado) return;
            if (cliente.terminated()) {
                detener();
                return;
            }
            try {
                String estado = pedidoDAO.consultarEstadoActual(idPedido);
                if (estado == null) {
                    cliente.sendEvent("error", Map.of("mensaje", "No existe el pedido " + idPedido + "."));
                    detener();
                    cliente.close();
                    return;
                }
                boolean esFinal = ESTADOS_FINALES.contains(estado);
                Map<String, Object> datos = new LinkedHashMap<>();
                datos.put("idPedido", idPedido);
                datos.put("estado", estado);
                datos.put("final", esFinal);
                datos.put("historial", mapaHistorial(estadoPedidoDAO.listarPorPedido(idPedido)));
                datos.put("servidorAhora", System.currentTimeMillis());
                cliente.sendEvent("estado", datos);

                if (esFinal) {
                    detener();
                    cliente.close();
                }
            } catch (DatosException e) {
                // Se reintenta en el proximo ciclo; el cliente ve el error pero la conexion sigue abierta
                cliente.sendEvent("error", Map.of("mensaje", e.getMessage()));
            } catch (RuntimeException e) {
                detener();
                cliente.close();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Conversion a JSON
    // ---------------------------------------------------------------------

    private static Map<String, Object> mapaProducto(Producto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idProducto", p.getIdProducto());
        m.put("nombre", p.getNombre());
        m.put("categoria", p.getCategoria());
        m.put("precioUnitario", p.getPrecioUnitario());
        m.put("stockActual", p.getStockActual());
        return m;
    }

    private static Map<String, Object> mapaCliente(Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCliente", c.getIdCliente());
        m.put("nombre", c.getNombre());
        m.put("email", c.getEmail());
        m.put("telefono", c.getTelefono());
        m.put("direccion", c.getDireccion());
        return m;
    }

    private static Map<String, Object> mapaPedido(Pedido p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idPedido", p.getIdPedido());
        m.put("idCliente", p.getIdCliente());
        m.put("nombreCliente", p.getNombreCliente());
        m.put("direccionCliente", p.getDireccionCliente());
        m.put("fechaCreacion", p.getFechaCreacion());
        m.put("timestampCreacion", aEpochMillis(p.getFechaCreacion()));
        m.put("estadoActual", p.getEstadoActual());
        m.put("total", p.calcularTotal());
        List<Map<String, Object>> detalles = new ArrayList<>();
        for (DetallePedido d : p.getDetalles()) {
            Map<String, Object> det = new LinkedHashMap<>();
            det.put("idProducto", d.getIdProducto());
            det.put("nombreProducto", d.getNombreProducto());
            det.put("cantidad", d.getCantidad());
            det.put("precioUnitario", d.getPrecioUnitario());
            det.put("subtotal", d.getSubtotal());
            detalles.add(det);
        }
        m.put("detalles", detalles);
        return m;
    }

    private static List<Map<String, Object>> mapaHistorial(List<EstadoPedido> historial) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (EstadoPedido e : historial) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("estado", e.getEstado());
            m.put("fechaHora", e.getFechaHora());
            m.put("timestamp", aEpochMillis(e.getFechaHora()));
            m.put("usuario", e.getNombreUsuario());
            m.put("observacion", e.getObservacion());
            lista.add(m);
        }
        return lista;
    }

    // El driver JDBC convierte los DATETIME a la zona horaria de la JVM
    private static Long aEpochMillis(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private int obtenerIdUsuarioPortal() throws DatosException {
        if (idUsuarioPortal <= 0) {
            int id = usuarioDAO.buscarIdPorNombre(nombreUsuarioPortal);
            if (id <= 0) {
                throw new DatosException("No existe el usuario '" + nombreUsuarioPortal
                    + "' para registrar pedidos web. Ejecutar sql/portal_web.sql.");
            }
            idUsuarioPortal = id;
        }
        return idUsuarioPortal;
    }

    private static <T> T leerCuerpo(Context ctx, Class<T> tipo) throws ValidacionException {
        try {
            T cuerpo = ctx.bodyAsClass(tipo);
            if (cuerpo == null) throw new ValidacionException("El cuerpo de la solicitud esta vacio.");
            return cuerpo;
        } catch (ValidacionException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidacionException("El cuerpo de la solicitud no es un JSON valido.");
        }
    }

    private static int leerIdPedido(Context ctx) throws ValidacionException {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ValidacionException("Identificador de pedido invalido.");
        }
    }

    private static String normalizarEmail(String email) throws ValidacionException {
        String limpio = email == null ? "" : email.trim().toLowerCase();
        if (limpio.length() > 100 || !PATRON_EMAIL.matcher(limpio).matches()) {
            throw new ValidacionException("Ingresa un email valido.");
        }
        return limpio;
    }

    private static String textoObligatorio(String valor, String campo, int maximo) throws ValidacionException {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) throw new ValidacionException("El campo " + campo + " es obligatorio.");
        if (limpio.length() > maximo) {
            throw new ValidacionException("El campo " + campo + " admite hasta " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static String textoOpcional(String valor, String campo, int maximo) throws ValidacionException {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.length() > maximo) {
            throw new ValidacionException("El campo " + campo + " admite hasta " + maximo + " caracteres.");
        }
        return limpio.isEmpty() ? null : limpio;
    }

    private static void error(Context ctx, HttpStatus estado, String mensaje) {
        ctx.status(estado).json(Map.of("error", mensaje));
    }

    private static String leerEntorno(String nombre, String valorPorDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? valorPorDefecto : valor;
    }
}
