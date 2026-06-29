package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;
import bioroute.modelo.EstadoPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * DAO para los cambios de estado de los pedidos.
 * Implementa la trazabilidad mediante INSERT en estado_pedido
 * y UPDATE en pedidos dentro de la misma operacion.
 */
public class EstadoPedidoDAO {

    public void registrarCambioEstado(int idPedido, String estado, int idUsuario, String observacion)
            throws DatosException {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);

            // Insertar el cambio en el historial
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO estado_pedido (id_pedido, estado, id_usuario, observacion) " +
                    "VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, idPedido);
                stmt.setString(2, estado);
                stmt.setInt(3, idUsuario);
                stmt.setString(4, observacion);
                stmt.executeUpdate();
            }

            // Actualizar el estado actual del pedido
            try (PreparedStatement stmt = con.prepareStatement(
                    "UPDATE pedidos SET estado_actual = ? WHERE id_pedido = ?")) {
                stmt.setString(1, estado);
                stmt.setInt(2, idPedido);
                stmt.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignored) { }
            throw new DatosException("Error al registrar cambio de estado.", e);
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) { }
        }
    }

    public ArrayList<EstadoPedido> listarPorPedido(int idPedido) throws DatosException {
        String sql = "SELECT ep.id_estado, ep.id_pedido, ep.estado, ep.fecha_hora, " +
                     "ep.id_usuario, u.nombre_usuario, ep.observacion " +
                     "FROM estado_pedido ep " +
                     "INNER JOIN usuarios u ON u.id_usuario = ep.id_usuario " +
                     "WHERE ep.id_pedido = ? " +
                     "ORDER BY ep.fecha_hora ASC";

        ArrayList<EstadoPedido> historial = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    historial.add(new EstadoPedido(
                        rs.getInt("id_estado"),
                        rs.getInt("id_pedido"),
                        rs.getString("estado"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("observacion")
                    ));
                }
            }
            return historial;
        } catch (SQLException e) {
            throw new DatosException("Error al consultar trazabilidad.", e);
        }
    }
}
