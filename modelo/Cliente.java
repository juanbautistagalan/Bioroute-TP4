package bioroute.modelo;

public class Cliente {
    private int idCliente;
    private String nombre;
    private String telefono;
    private String direccion;
    private String email;

    public Cliente(String nombre, String telefono, String direccion, String email) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    public Cliente(int idCliente, String nombre, String telefono, String direccion, String email) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    public int getIdCliente() { return idCliente; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return String.format("[%d] %s | Tel: %s | %s | %s",
            idCliente, nombre, telefono, direccion, email);
    }
}
