package bioroute.modelo;

/**
 * Usuario con perfil Administrativo. Tiene acceso a todas las funcionalidades.
 */
public class Administrativo extends Usuario {

    public Administrativo(int idUsuario, String nombreUsuario, String perfil) {
        super(idUsuario, nombreUsuario, perfil);
    }

    @Override
    public void mostrarMenu() {
        System.out.println();
        System.out.println("=== MENU ADMINISTRATIVO ===");
        System.out.println("1.  Registrar producto");
        System.out.println("2.  Listar productos");
        System.out.println("3.  Buscar producto por nombre");
        System.out.println("4.  Registrar cliente");
        System.out.println("5.  Listar clientes");
        System.out.println("6.  Crear pedido");
        System.out.println("7.  Actualizar estado de pedido");
        System.out.println("8.  Consultar trazabilidad");
        System.out.println("9.  Productos con stock bajo");
        System.out.println("10. Ordenar productos por stock");
        System.out.println("11. Probar conexion MySQL");
        System.out.println("0.  Salir");
    }

    @Override
    public boolean puedeEjecutar(int opcion) {
        // El administrativo puede ejecutar todas las opciones
        return opcion >= 0 && opcion <= 11;
    }
}
