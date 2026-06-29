package bioroute.config;

import bioroute.exception.DatosException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexion a MySQL para BioRoute.
 * Lee las credenciales desde constantes para simplificar el prototipo academico.
 */
public final class ConexionBD {

    // =====================================================================
    // EDITAR ESTAS 3 LINEAS CON LOS DATOS DE TU MYSQL LOCAL
    // =====================================================================
    private static final String URL =
        "jdbc:mysql://localhost:3306/bioroute_db?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <-- poner tu password real de MySQL
    // =====================================================================

    private ConexionBD() {
        // utilitaria, no se instancia
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
