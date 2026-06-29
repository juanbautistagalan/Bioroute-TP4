package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;
import bioroute.modelo.DetallePedido;
import bioroute.modelo.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO de Pedido. Maneja la creacion del pedido dentro de una transaccion JDBC
 * que involucra varias tablas: pedidos, detalle_pedido, productos, estado_pedido.
 */
public class PedidoDAO {

    public int crear(Pedido pedido) throws DatosException {
        Connection con = null;
        try {
            con = ConexionBD.obtenerConexion();
            con.setAutoCommit(false);  // inicio de transaccion

            // 1. Validar stock disponible
            for (DetallePedido det : pedido.getDetalles()) {
                int stockDisponible = consultarStock(con, det.getIdProducto());
                if (stockDisponible < det.getCantidad()) {
                    throw new DatosException(
                        "Stock insuficiente para el producto '" + det.getNombreProducto() +
                        "' (disponible: " + stockDisponible + ", solicitado: " + det.getCantidad() + ").");
                }
            }

            // 2. Insertar pedido cabecera
            int idPedido = insertarPedido(con, pedido);
            pedido.setIdPedido(idPedido);

            // 3. Insertar detalles y descontar stock
            for (DetallePedido det : pedido.getDetalles()) {
                insertarDetalle(con, idPedido, det);
                descontarStock(con, det.getIdProducto(), det.getCantidad());
            }

            // 4. Insertar estado inicial
            insertarEstadoInicial(con, idPedido, pedido.getIdUsuario());

            con.commit();  // confirmacion de la transaccion
            return idPedido;

        } catch (SQLException e) {
            rollback(con);
            throw new DatosException("Error al crear el pedido (transaccion revertida).", e);
        } catch (DatosException e) {
            rollback(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    private int consultarStock(Connection con, int idProducto) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT stock_actual FROM productos WHERE id_producto = ?")) {
            stmt.setInt(1, idProducto);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }

    private int insertarPedido(Connection con, Pedido pedido) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO pedidos (id_cliente, id_usuario, estado_actual) VALUES (?, ?, 'Pendiente')",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pedido.getIdCliente());
            stmt.setInt(2, pedido.getIdUsuario());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No se pudo recuperar el ID del pedido.");
            }
        }
    }

    private void insertarDetalle(Connection con, int idPedido, DetallePedido det) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, subtotal) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, idPedido);
            stmt.setInt(2, det.getIdProducto());
            stmt.setInt(3, det.getCantidad());
            stmt.setDouble(4, det.getSubtotal());
            stmt.executeUpdate();
        }
    }

    private void descontarStock(Connection con, int idProducto, int cantidad) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "UPDATE productos SET stock_actual = stock_actual - ? WHERE id_producto = ?")) {
            stmt.setInt(1, cantidad);
            stmt.setInt(2, idProducto);
            stmt.executeUpdate();
        }
    }

    private void insertarEstadoInicial(Connection con, int idPedido, int idUsuario) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO estado_pedido (id_pedido, estado, id_usuario, observacion) " +
                "VALUES (?, 'Pendiente', ?, 'Pedido creado')")) {
            stmt.setInt(1, idPedido);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    private void rollback(Connection con) {
        if (con != null) {
            try { con.rollback(); } catch (SQLException ignored) { }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) { }
        }
    }
}
