package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;
import bioroute.modelo.Administrativo;
import bioroute.modelo.Repartidor;
import bioroute.modelo.Usuario;
import bioroute.util.HashUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO para autenticacion y consulta de usuarios.
 */
public class UsuarioDAO {

    public Optional<Usuario> autenticar(String nombreUsuario, String contrasena) throws DatosException {
        String sql = "SELECT id_usuario, nombre_usuario, hash_contrasena, sal, perfil " +
                     "FROM usuarios WHERE nombre_usuario = ?";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, nombreUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String hash = rs.getString("hash_contrasena");
                String sal = rs.getString("sal");

                if (!HashUtil.verificar(contrasena, sal, hash)) {
                    return Optional.empty();
                }

                int id = rs.getInt("id_usuario");
                String nombre = rs.getString("nombre_usuario");
                String perfil = rs.getString("perfil");

                Usuario usuario;
                if ("Administrativo".equalsIgnoreCase(perfil)) {
                    usuario = new Administrativo(id, nombre, perfil);
                } else {
                    usuario = new Repartidor(id, nombre, perfil);
                }
                return Optional.of(usuario);
            }
        } catch (SQLException e) {
            throw new DatosException("Error al autenticar usuario.", e);
        }
    }
}
