package bioroute.modelo;

/**
 * Usuario con perfil Repartidor. Solo puede consultar y actualizar estados.
 */
public class Repartidor extends Usuario {

    public Repartidor(int idUsuario, String nombreUsuario, String perfil) {
        super(idUsuario, nombreUsuario, perfil);
    }

    @Override
    public void mostrarMenu() {
        System.out.println();
        System.out.println("=== MENU REPARTIDOR ===");
        System.out.println("2.  Listar productos");
        System.out.println("7.  Actualizar estado de pedido");
        System.out.println("8.  Consultar trazabilidad");
        System.out.println("11. Probar conexion MySQL");
        System.out.println("0.  Salir");
    }

    @Override
    public boolean puedeEjecutar(int opcion) {
        // El repartidor solo puede ejecutar opciones limitadas
        return opcion == 0 || opcion == 2 || opcion == 7 || opcion == 8 || opcion == 11;
    }
}
