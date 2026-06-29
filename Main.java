package bioroute;

import bioroute.config.ConexionBD;
import bioroute.dao.ClienteDAO;
import bioroute.dao.EstadoPedidoDAO;
import bioroute.dao.PedidoDAO;
import bioroute.dao.ProductoDAO;
import bioroute.dao.UsuarioDAO;
import bioroute.exception.DatosException;
import bioroute.exception.ValidacionException;
import bioroute.modelo.Administrativo;
import bioroute.modelo.Cliente;
import bioroute.modelo.DetallePedido;
import bioroute.modelo.EstadoPedido;
import bioroute.modelo.Pedido;
import bioroute.modelo.Producto;
import bioroute.modelo.Repartidor;
import bioroute.modelo.Usuario;
import bioroute.util.AuditoriaArchivo;
import bioroute.util.Validador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private final Scanner scanner;
    private final UsuarioDAO usuarioDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final PedidoDAO pedidoDAO;
    private final EstadoPedidoDAO estadoPedidoDAO;

    public Main() {
        this.scanner = new Scanner(System.in);
        this.usuarioDAO = new UsuarioDAO();
        this.productoDAO = new ProductoDAO();
        this.clienteDAO = new ClienteDAO();
        this.pedidoDAO = new PedidoDAO();
        this.estadoPedidoDAO = new EstadoPedidoDAO();
    }

    public static void main(String[] args) {
        new Main().ejecutar();
    }

    private void ejecutar() {
        try {
            Usuario usuario = iniciarSesion();
            System.out.println("Bienvenido, " + usuario.getNombreUsuario() + " (" + usuario.getPerfil() + ")");
            AuditoriaArchivo.registrar("Inicio de sesion: " + usuario.getNombreUsuario());

            demostrarPolimorfismo();

            int opcion = -1;
            while (opcion != 0) {
                usuario.mostrarMenu();
                try {
                    opcion = Validador.leerEntero(scanner, "Seleccione una opcion: ");
                    if (!usuario.puedeEjecutar(opcion)) {
                        System.out.println("No tiene permiso para ejecutar esta opcion.");
                        continue;
                    }
                    procesarOpcion(opcion, usuario);
                } catch (ValidacionException e) {
                    System.out.println("Error de validacion: " + e.getMessage());
                } catch (DatosException e) {
                    System.out.println("Error de datos: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.out.println("Detalle tecnico: " + e.getCause().getMessage());
                    }
                }
            }
        } catch (DatosException e) {
            System.out.println("No se pudo iniciar BioRoute: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Detalle tecnico: " + e.getCause().getMessage());
            }
        } finally {
            scanner.close();
        }
    }

    private Usuario iniciarSesion() throws DatosException {
        System.out.println("=== BIOROUTE - INICIO DE SESION ===");
        System.out.print("Usuario: ");
        String nombreUsuario = scanner.nextLine().trim();
        System.out.print("Contrasena: ");
        String contrasena = scanner.nextLine().trim();

        Optional<Usuario> usuario = usuarioDAO.autenticar(nombreUsuario, contrasena);
        if (usuario.isEmpty()) {
            throw new DatosException("Usuario o contrasena incorrectos.");
        }
        return usuario.get();
    }

    /**
     * Demuestra polimorfismo con una coleccion de Usuario.
     * En tiempo de ejecucion se invoca el metodo mostrarMenu() de la clase concreta.
     */
    private void demostrarPolimorfismo() {
        List<Usuario> ejemplos = new ArrayList<>();
        ejemplos.add(new Administrativo(0, "demo_admin", "Administrativo"));
        ejemplos.add(new Repartidor(0, "demo_reparto", "Repartidor"));
        System.out.println();
        System.out.println("[Demostracion de polimorfismo - menus por perfil]");
        for (Usuario u : ejemplos) {
            u.mostrarMenu();
        }
        System.out.println();
        System.out.println("[Fin de la demostracion. Comienza tu sesion.]");
    }

    private void procesarOpcion(int opcion, Usuario usuario) throws DatosException, ValidacionException {
        switch (opcion) {
            case 1:  registrarProducto(); break;
            case 2:  listarProductos(productoDAO.listar()); break;
            case 3:  buscarProducto(); break;
            case 4:  registrarCliente(); break;
            case 5:  listarClientes(); break;
            case 6:  crearPedido(usuario); break;
            case 7:  actualizarEstado(usuario); break;
            case 8:  consultarTrazabilidad(); break;
            case 9:  listarProductos(productoDAO.listarStockBajo()); break;
            case 10: listarProductos(productoDAO.listarOrdenadosPorStock()); break;
            case 11: probarConexion(); break;
            case 0:
                System.out.println("Saliendo del sistema BioRoute...");
                AuditoriaArchivo.registrar("Cierre de sesion: " + usuario.getNombreUsuario());
                break;
            default:
                System.out.println("Opcion invalida. Intente nuevamente.");
        }
    }

    private void registrarProducto() throws ValidacionException, DatosException {
        String nombre = Validador.leerTextoObligatorio(scanner, "Nombre: ");
        String categoria = Validador.leerTextoObligatorio(scanner, "Categoria: ");
        double precio = Validador.leerDecimal(scanner, "Precio unitario: ");
        int stockActual = Validador.leerEntero(scanner, "Stock actual: ");
        int stockMinimo = Validador.leerEntero(scanner, "Stock minimo: ");
        Validador.validarPositivo(precio, "El precio");

        Producto producto = new Producto(nombre, categoria, precio, stockActual, stockMinimo);
        int id = productoDAO.crear(producto);
        System.out.println("Producto registrado con ID " + id);
        AuditoriaArchivo.registrar("Producto registrado: " + nombre);
    }

    private void listarProductos(ArrayList<Producto> productos) {
        if (productos.isEmpty()) {
            System.out.println("No hay productos para mostrar.");
            return;
        }
        for (Producto p : productos) System.out.println(p);
    }

    private void buscarProducto() throws ValidacionException, DatosException {
        String nombre = Validador.leerTextoObligatorio(scanner, "Nombre o parte del nombre: ");
        Producto p = productoDAO.buscarPorNombre(nombre);
        if (p == null) System.out.println("No se encontraron productos con ese nombre.");
        else System.out.println(p);
    }

    private void registrarCliente() throws ValidacionException, DatosException {
        String nombre = Validador.leerTextoObligatorio(scanner, "Nombre: ");
        System.out.print("Telefono: ");
        String telefono = scanner.nextLine().trim();
        String direccion = Validador.leerTextoObligatorio(scanner, "Direccion: ");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        Cliente cliente = new Cliente(nombre, telefono, direccion, email);
        int id = clienteDAO.crear(cliente);
        System.out.println("Cliente registrado con ID " + id);
        AuditoriaArchivo.registrar("Cliente registrado: " + nombre);
    }

    private void listarClientes() throws DatosException {
        ArrayList<Cliente> clientes = clienteDAO.listar();
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados.");
            return;
        }
        for (Cliente c : clientes) System.out.println(c);
    }

    private void crearPedido(Usuario usuario) throws DatosException, ValidacionException {
        listarClientes();
        int idCliente = Validador.leerEntero(scanner, "ID del cliente: ");
        Validador.validarPositivo(idCliente, "El ID del cliente");

        Pedido pedido = new Pedido(idCliente, usuario.getIdUsuario());
        boolean seguir = true;

        while (seguir) {
            listarProductos(productoDAO.listar());
            int idProducto = Validador.leerEntero(scanner, "ID del producto: ");
            int cantidad = Validador.leerEntero(scanner, "Cantidad: ");
            Validador.validarPositivo(cantidad, "La cantidad");

            Producto p = buscarEnLista(idProducto, productoDAO.listar());
            if (p == null) {
                System.out.println("No existe el producto indicado.");
            } else {
                pedido.agregarDetalle(new DetallePedido(idProducto, p.getNombre(), cantidad, p.getPrecioUnitario()));
                System.out.println("Producto agregado al pedido.");
            }

            System.out.print("Agregar otro producto? (s/n): ");
            seguir = scanner.nextLine().trim().equalsIgnoreCase("s");
        }

        int idPedido = pedidoDAO.crear(pedido);
        System.out.println("Pedido creado con ID " + idPedido +
            ". Total estimado: $" + String.format("%.2f", pedido.calcularTotal()));
        AuditoriaArchivo.registrar("Pedido creado: " + idPedido);
    }

    private Producto buscarEnLista(int idProducto, ArrayList<Producto> lista) {
        for (Producto p : lista) {
            if (p.getIdProducto() == idProducto) return p;
        }
        return null;
    }

    private void actualizarEstado(Usuario usuario) throws ValidacionException, DatosException {
        int idPedido = Validador.leerEntero(scanner, "ID del pedido: ");
        mostrarEstadosValidos();
        String estado = Validador.leerTextoObligatorio(scanner, "Nuevo estado: ");
        Validador.validarEstado(estado);
        System.out.print("Observacion: ");
        String observacion = scanner.nextLine().trim();

        estadoPedidoDAO.registrarCambioEstado(idPedido, capitalizar(estado), usuario.getIdUsuario(), observacion);
        System.out.println("Estado actualizado correctamente.");
        AuditoriaArchivo.registrar("Cambio de estado pedido " + idPedido + ": " + estado);
    }

    private String capitalizar(String texto) {
        for (String valido : EstadoPedido.ESTADOS_VALIDOS) {
            if (valido.equalsIgnoreCase(texto)) return valido;
        }
        return texto;
    }

    private void consultarTrazabilidad() throws ValidacionException, DatosException {
        int idPedido = Validador.leerEntero(scanner, "ID del pedido: ");
        ArrayList<EstadoPedido> historial = estadoPedidoDAO.listarPorPedido(idPedido);
        if (historial.isEmpty()) {
            System.out.println("No hay trazabilidad registrada para el pedido indicado.");
            return;
        }
        System.out.println();
        System.out.println("=== TRAZABILIDAD DEL PEDIDO " + idPedido + " ===");
        for (EstadoPedido ep : historial) {
            System.out.println(ep);
        }
    }

    private void mostrarEstadosValidos() {
        System.out.println("Estados validos:");
        for (int i = 0; i < EstadoPedido.ESTADOS_VALIDOS.length; i++) {
            System.out.println("  " + (i + 1) + ". " + EstadoPedido.ESTADOS_VALIDOS[i]);
        }
    }

    private void probarConexion() throws DatosException {
        if (ConexionBD.probarConexion()) {
            System.out.println("Conexion MySQL validada correctamente.");
        }
    }
}
