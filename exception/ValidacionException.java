package bioroute.exception;

/**
 * Excepcion personalizada para errores de validacion de entrada del usuario.
 */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
