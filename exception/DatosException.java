package bioroute.exception;

/**
 * Excepcion personalizada para errores de acceso a datos.
 * Encapsula problemas de conexion, ejecucion de queries o transacciones JDBC.
 */
public class DatosException extends Exception {

    public DatosException(String mensaje) {
        super(mensaje);
    }

    public DatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
