package bioroute.modelo;

/**
 * Clase abstracta que representa un usuario del sistema BioRoute.
 * Aplica abstraccion y sirve como base para Administrativo y Repartidor.
 */
public abstract class Usuario {
    protected int idUsuario;
    protected String nombreUsuario;
    protected String perfil;

    public Usuario(int idUsuario, String nombreUsuario, String perfil) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.perfil = perfil;
    }

    public abstract void mostrarMenu();

    public abstract boolean puedeEjecutar(int opcion);

    public int getIdUsuario() { return idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getPerfil() { return perfil; }
}
