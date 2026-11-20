package bioroute.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Entidad Pedido. Compone una lista de DetallePedido (composicion POO).
 */
public class Pedido {
    private int idPedido;
    private int idCliente;
    private int idUsuario;
    private LocalDateTime fechaCreacion;
    private String estadoActual;
    private ArrayList<DetallePedido> detalles;
    private String nombreCliente;
    private String direccionCliente;

    public Pedido(int idCliente, int idUsuario) {
        this.idCliente = idCliente;
        this.idUsuario = idUsuario;
        this.fechaCreacion = LocalDateTime.now();
        this.estadoActual = "Pendiente";
        this.detalles = new ArrayList<>();
    }

    // Constructor para hidratar desde la base de datos
    public Pedido(int idPedido, int idCliente, int idUsuario, LocalDateTime fechaCreacion,
                  String estadoActual, String nombreCliente, String direccionCliente) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.idUsuario = idUsuario;
        this.fechaCreacion = fechaCreacion;
        this.estadoActual = estadoActual;
        this.nombreCliente = nombreCliente;
        this.direccionCliente = direccionCliente;
        this.detalles = new ArrayList<>();
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
    }

    public double calcularTotal() {
        double total = 0;
        for (DetallePedido d : detalles) {
            total += d.getSubtotal();
        }
        return total;
    }

    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }
    public int getIdCliente() { return idCliente; }
    public int getIdUsuario() { return idUsuario; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public String getEstadoActual() { return estadoActual; }
    public ArrayList<DetallePedido> getDetalles() { return detalles; }
    public String getNombreCliente() { return nombreCliente; }
    public String getDireccionCliente() { return direccionCliente; }
}
