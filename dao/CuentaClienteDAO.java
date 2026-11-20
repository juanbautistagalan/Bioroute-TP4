package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * DAO de las credenciales de acceso de los clientes al portal web.
 * La contrasena se guarda como hash SHA-256 con sal (igual que los usuarios internos).
 */
public class CuentaClienteDAO {

    public record Cuenta(int idCliente, String email, String hashContrasena, String sal) { }

    public Cuenta buscarPorEmail(String email) throws DatosException {
        String sql = "SELECT id_cliente, email, hash_contrasena, sal FROM cuentas_cliente WHERE email = ?";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                return new Cuenta(
                    rs.getInt("id_cliente"),
                    rs.getString("email"),
                    rs.getString("hash_contrasena"),
                    rs.getString("sal"));
            }
        } catch (SQLException e) {
            throw new DatosException("Error al buscar la cuenta del cliente.", e);
        }
    }

    /**
     * Crea la cuenta. Devuelve false si ya existe una cuenta con ese email o para ese cliente.
     */
    public boolean crear(int idCliente, String email, String hashContrasena, String sal) throws DatosException {
        String sql = "INSERT INTO cuentas_cliente (id_cliente, email, hash_contrasena, sal) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            stmt.setString(2, email);
            stmt.setString(3, hashContrasena);
            stmt.setString(4, sal);
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            throw new DatosException("Error al crear la cuenta del cliente.", e);
        }
    }
}
