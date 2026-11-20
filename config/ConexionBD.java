package bioroute.config;

import bioroute.exception.DatosException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexion a MySQL para BioRoute.
 * Lee las credenciales desde variables de entorno, con valores por defecto
 * para el entorno local del prototipo academico.
 */
public final class ConexionBD {

    // =====================================================================
    // Las credenciales se leen de variables de entorno. Si no estan
    // definidas se usan estos valores por defecto (MySQL local).
    //   BIOROUTE_DB_URL, BIOROUTE_DB_USER, BIOROUTE_DB_PASSWORD
    // =====================================================================
    private static final String URL_DEFECTO =
        "jdbc:mysql://localhost:3306/bioroute_db?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true";
    private static final String USER_DEFECTO = "root";
    private static final String PASSWORD_DEFECTO = "1234"; // <-- o definir BIOROUTE_DB_PASSWORD

    private static final String URL = leerEntorno("BIOROUTE_DB_URL", URL_DEFECTO);
    private static final String USER = leerEntorno("BIOROUTE_DB_USER", USER_DEFECTO);
    private static final String PASSWORD = leerEntorno("BIOROUTE_DB_PASSWORD", PASSWORD_DEFECTO);
    // =====================================================================

    private ConexionBD() {
        // utilitaria, no se instancia
    }

    private static String leerEntorno(String nombre, String valorPorDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? valorPorDefecto : valor;
    }

    public static Connection obtenerConexion() throws DatosException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new DatosException(
                "No se encontro el driver JDBC de MySQL. Verifica que el jar este en el classpath.", e);
        } catch (SQLException e) {
            throw new DatosException(
                "No fue posible conectar con la base de datos MySQL.", e);
        }
    }

    public static boolean probarConexion() throws DatosException {
        try (Connection conexion = obtenerConexion()) {
            return conexion != null && !conexion.isClosed();
        } catch (SQLException e) {
            throw new DatosException("Error al probar la conexion.", e);
        }
    }
}
