package bioroute.modelo;

/**
 * Entidad Producto del dominio. Aplica encapsulamiento con atributos privados.
 */
public class Producto {
    private int idProducto;
    private String nombre;
    private String categoria;
    private double precioUnitario;
    private int stockActual;
    private int stockMinimo;

    // Constructor para creacion desde consola (sin ID, lo genera la base)
    public Producto(String nombre, String categoria, double precioUnitario, int stockActual, int stockMinimo) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioUnitario = precioUnitario;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    // Constructor para hidratar desde la base de datos (con ID)
    public Producto(int idProducto, String nombre, String categoria, double precioUnitario, int stockActual, int stockMinimo) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioUnitario = precioUnitario;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public double getPrecioUnitario() { return precioUnitario; }
    public int getStockActual() { return stockActual; }
    public int getStockMinimo() { return stockMinimo; }

    public void setPrecioUnitario(double precio) {
        if (precio <= 0) throw new IllegalArgumentException("El precio debe ser positivo.");
        this.precioUnitario = precio;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - $%.2f - Stock: %d (min: %d)",
            idProducto, nombre, categoria, precioUnitario, stockActual, stockMinimo);
    }
}
