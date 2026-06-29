package bioroute.util;

import bioroute.exception.ValidacionException;
import bioroute.modelo.EstadoPedido;
import java.util.Scanner;

/**
 * Utilidad para validar y leer datos por consola.
 */
public final class Validador {

    private Validador() { }

    public static int leerEntero(Scanner scanner, String mensaje) throws ValidacionException {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim();
        try {
            return Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            throw new ValidacionException("Debe ingresar un numero entero valido.");
        }
    }

    public static double leerDecimal(Scanner scanner, String mensaje) throws ValidacionException {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim().replace(",", ".");
        try {
            return Double.parseDouble(entrada);
        } catch (NumberFormatException e) {
            throw new ValidacionException("Debe ingresar un numero decimal valido.");
        }
    }

    public static String leerTextoObligatorio(Scanner scanner, String mensaje) throws ValidacionException {
        System.out.print(mensaje);
        String entrada = scanner.nextLine().trim();
        if (entrada.isEmpty()) {
            throw new ValidacionException("Este campo es obligatorio.");
        }
        return entrada;
    }

    public static void validarPositivo(double valor, String campo) throws ValidacionException {
        if (valor <= 0) {
            throw new ValidacionException(campo + " debe ser un valor positivo.");
        }
    }

    public static void validarEstado(String estado) throws ValidacionException {
        for (String valido : EstadoPedido.ESTADOS_VALIDOS) {
            if (valido.equalsIgnoreCase(estado)) {
                return;
            }
        }
        throw new ValidacionException("Estado invalido. Debe ser uno de los listados.");
    }
}
