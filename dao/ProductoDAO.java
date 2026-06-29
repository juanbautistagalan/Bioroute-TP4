package bioroute.dao;

import bioroute.config.ConexionBD;
import bioroute.exception.DatosException;
import bioroute.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * DAO de Producto. Implementa CrudRepository<Producto>.
 */
public class ProductoDAO implements CrudRepository<Producto> {

    @Override
    public int crear(Producto producto) throws DatosException {
        String sql = "INSERT INTO productos (nombre, categoria, precio_unitario, stock_actual, stock_minimo) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getCategoria());
            stmt.setDouble(3, producto.getPrecioUnitario());
            stmt.setInt(4, producto.getStockActual());
            stmt.setInt(5, producto.getStockMinimo());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                return -1;
            }
        } catch (SQLException e) {
            throw new DatosException("Error al registrar producto.", e);
        }
    }

    @Override
    public ArrayList<Producto> listar() throws DatosException {
        String sql = "SELECT id_producto, nombre, categoria, precio_unitario, stock_actual, stock_minimo " +
                     "FROM productos ORDER BY id_producto";
        ArrayList<Producto> productos = new ArrayList<>();

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                productos.add(mapear(rs));
            }
            return productos;
        } catch (SQLException e) {
            throw new DatosException("Error al listar productos.", e);
        }
    }

    public Producto buscarPorNombre(String nombre) throws DatosException {
        String sql = "SELECT id_producto, nombre, categoria, precio_unitario, stock_actual, stock_minimo " +
                     "FROM productos WHERE nombre LIKE ? LIMIT 1";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new DatosException("Error al buscar producto.", e);
        }
    }

    public ArrayList<Producto> listarStockBajo() throws DatosException {
        String sql = "SELECT id_producto, nombre, categoria, precio_unitario, stock_actual, stock_minimo " +
                     "FROM productos WHERE stock_actual <= stock_minimo";
        return ejecutarQuery(sql);
    }

    public ArrayList<Producto> listarOrdenadosPorStock() throws DatosException {
        String sql = "SELECT id_producto, nombre, categoria, precio_unitario, stock_actual, stock_minimo " +
                     "FROM productos ORDER BY stock_actual ASC";
        return ejecutarQuery(sql);
    }

    private ArrayList<Producto> ejecutarQuery(String sql) throws DatosException {
        ArrayList<Producto> productos = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) productos.add(mapear(rs));
            return productos;
        } catch (SQLException e) {
            throw new DatosException("Error al ejecutar consulta de productos.", e);
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
            rs.getInt("id_producto"),
            rs.getString("nombre"),
            rs.getString("categoria"),
            rs.getDouble("precio_unitario"),
            rs.getInt("stock_actual"),
            rs.getInt("stock_minimo")
        );
    }
}
