package bioroute.modelo;

import java.time.LocalDateTime;

/**
 * Representa un cambio de estado de un pedido para trazabilidad.
 * Usa un arreglo (no ArrayList) para los estados validos porque son fijos.
 */
public class EstadoPedido {

    // Arreglo de estados validos. Uso complementario con ArrayList (TP4).
    public static final String[] ESTADOS_VALIDOS = {
        "Pendiente", "En preparacion", "En ruta", "Entregado", "Cancelado"
    };

    private int idEstado;
    private int idPedido;
    private String estado;
    private LocalDateTime fechaHora;
    private int idUsuario;
    private String nombreUsuario;
    private String observacion;

    public EstadoPedido(int idEstado, int idPedido, String estado, LocalDateTime fechaHora,
                        int idUsuario, String nombreUsuario, String observacion) {
        this.idEstado = idEstado;
        this.idPedido = idPedido;
        this.estado = estado;
        this.fechaHora = fechaHora;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.observacion = observacion;
    }

    public int getIdEstado() { return idEstado; }
    public int getIdPedido() { return idPedido; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getObservacion() { return observacion; }

    @Override
    public String toString() {
        return String.format("[%d] %s - %s - %s - %s",
            idEstado, estado, fechaHora.toString().replace("T", " "),
            nombreUsuario, observacion == null ? "" : observacion);
    }
}
