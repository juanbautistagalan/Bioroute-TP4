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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final String SELECT_PEDIDOS =
        "SELECT p.id_pedido, p.id_cliente, p.id_usuario, p.fecha_creacion, p.estado_actual, " +
        "c.nombre AS nombre_cliente, c.direccion AS direccion_cliente, " +
        "d.id_producto, pr.nombre AS nombre_producto, d.cantidad, d.subtotal " +
        "FROM pedidos p " +
        "INNER JOIN clientes c ON c.id_cliente = p.id_cliente " +
        "LEFT JOIN detalle_pedido d ON d.id_pedido = p.id_pedido " +
        "LEFT JOIN productos pr ON pr.id_producto = d.id_producto ";

    /**
     * Lista los pedidos de un cliente (buscado por email) con su estado actual y detalles,
     * del mas reciente al mas antiguo.
     */
    public ArrayList<Pedido> listarPorEmailCliente(String email) throws DatosException {
        String sql = SELECT_PEDIDOS +
                     "WHERE c.email = ? " +
                     "ORDER BY p.fecha_creacion DESC, p.id_pedido DESC, d.id_detalle ASC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email);
            return mapearPedidos(stmt);
        } catch (SQLException e) {
            throw new DatosException("Error al listar pedidos del cliente.", e);
        }
    }

    /**
     * Busca un pedido por id con sus detalles. Devuelve null si no existe.
     */
    public Pedido buscarPorId(int idPedido) throws DatosException {
        String sql = SELECT_PEDIDOS +
                     "WHERE p.id_pedido = ? " +
                     "ORDER BY d.id_detalle ASC";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, idPedido);
            ArrayList<Pedido> pedidos = mapearPedidos(stmt);
            return pedidos.isEmpty() ? null : pedidos.get(0);
        } catch (SQLException e) {
            throw new DatosException("Error al consultar el pedido.", e);
        }
    }

    /**
     * Devuelve el estado actual del pedido, o null si no existe.
     */
    public String consultarEstadoActual(int idPedido) throws DatosException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(
                 "SELECT estado_actual FROM pedidos WHERE id_pedido = ?")) {

            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new DatosException("Error al consultar el estado del pedido.", e);
        }
    }

    // Agrupa las filas (una por detalle) en objetos Pedido, respetando el orden de la consulta.
    private ArrayList<Pedido> mapearPedidos(PreparedStatement stmt) throws SQLException {
        Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int idPedido = rs.getInt("id_pedido");
                Pedido pedido = pedidos.get(idPedido);
                if (pedido == null) {
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    pedido = new Pedido(
                        idPedido,
                        rs.getInt("id_cliente"),
                        rs.getInt("id_usuario"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getString("estado_actual"),
                        rs.getString("nombre_cliente"),
                        rs.getString("direccion_cliente")
                    );
                    pedidos.put(idPedido, pedido);
                }
                int idProducto = rs.getInt("id_producto");
                if (!rs.wasNull()) {
                    int cantidad = rs.getInt("cantidad");
                    double subtotal = rs.getDouble("subtotal");
                    pedido.agregarDetalle(new DetallePedido(
                        idProducto,
                        rs.getString("nombre_producto"),
                        cantidad,
                        cantidad > 0 ? subtotal / cantidad : 0
                    ));
                }
            }
        }
        return new ArrayList<>(pedidos.values());
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
