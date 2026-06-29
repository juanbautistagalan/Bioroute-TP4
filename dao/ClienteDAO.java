package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;
import bioroute.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * DAO de Cliente. Implementa CrudRepository<Cliente>.
 */
public class ClienteDAO implements CrudRepository<Cliente> {

    @Override
    public int crear(Cliente cliente) throws DatosException {
        String sql = "INSERT INTO clientes (nombre, telefono, direccion, email) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getEmail());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                return -1;
            }
        } catch (SQLException e) {
            throw new DatosException("Error al registrar cliente.", e);
        }
    }

    @Override
    public ArrayList<Cliente> listar() throws DatosException {
        String sql = "SELECT id_cliente, nombre, telefono, direccion, email FROM clientes ORDER BY id_cliente";
        ArrayList<Cliente> clientes = new ArrayList<>();

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clientes.add(new Cliente(
                    rs.getInt("id_cliente"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("direccion"),
                    rs.getString("email")
                ));
            }
            return clientes;
        } catch (SQLException e) {
            throw new DatosException("Error al listar clientes.", e);
        }
    }
}
