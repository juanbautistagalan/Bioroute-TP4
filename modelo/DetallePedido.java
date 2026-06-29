package bioroute.modelo;

public class DetallePedido {
    private int idDetalle;
    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitario;

    public DetallePedido(int idProducto, String nombreProducto, int cantidad, double precioUnitario) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getSubtotal() { return cantidad * precioUnitario; }

    @Override
    public String toString() {
        return String.format("  - %s x%d @ $%.2f = $%.2f",
            nombreProducto, cantidad, precioUnitario, getSubtotal());
    }
}
