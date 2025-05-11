package com.mistrapitos.dao;

import com.mistrapitos.models.DetalleVenta;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad DetalleVenta.
 */
public class DetalleVentaDao implements Dao<DetalleVenta, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(DetalleVentaDao.class);
    
    /**
     * Obtiene todos los detalles de venta de la base de datos.
     * @return Lista de detalles de venta
     */
    @Override
    public List<DetalleVenta> findAll() {
        List<DetalleVenta> detalles = new ArrayList<>();
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, d.cantidad, d.precio_unitario, " +
                     "d.descuento_aplicado, p.nombre as producto_nombre " +
                     "FROM detalle_venta d " +
                     "LEFT JOIN productos p ON d.id_producto = p.id_producto " +
                     "ORDER BY d.id_venta, d.id_detalle";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                DetalleVenta detalle = mapResultSetToDetalleVenta(rs);
                detalles.add(detalle);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los detalles de venta", e);
        }
        
        return detalles;
    }
    
    /**
     * Obtiene un detalle de venta por su ID.
     * @param id ID del detalle de venta
     * @return Detalle de venta encontrado o vacío si no existe
     */
    @Override
    public Optional<DetalleVenta> findById(Integer id) {
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, d.cantidad, d.precio_unitario, " +
                     "d.descuento_aplicado, p.nombre as producto_nombre " +
                     "FROM detalle_venta d " +
                     "LEFT JOIN productos p ON d.id_producto = p.id_producto " +
                     "WHERE d.id_detalle = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    DetalleVenta detalle = mapResultSetToDetalleVenta(rs);
                    return Optional.of(detalle);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener detalle de venta por ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Busca detalles de venta por ID de venta.
     * @param idVenta ID de la venta
     * @return Lista de detalles de la venta
     */
    public List<DetalleVenta> findByVenta(Integer idVenta) {
        List<DetalleVenta> detalles = new ArrayList<>();
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, d.cantidad, d.precio_unitario, " +
                     "d.descuento_aplicado, p.nombre as producto_nombre " +
                     "FROM detalle_venta d " +
                     "LEFT JOIN productos p ON d.id_producto = p.id_producto " +
                     "WHERE d.id_venta = ? " +
                     "ORDER BY d.id_detalle";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idVenta);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta detalle = mapResultSetToDetalleVenta(rs);
                    detalles.add(detalle);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar detalles de venta por ID de venta: " + idVenta, e);
        }
        
        return detalles;
    }
    
    /**
     * Busca detalles de venta por ID de producto.
     * @param idProducto ID del producto
     * @return Lista de detalles de venta que contienen el producto
     */
    public List<DetalleVenta> findByProducto(Integer idProducto) {
        List<DetalleVenta> detalles = new ArrayList<>();
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, d.cantidad, d.precio_unitario, " +
                     "d.descuento_aplicado, p.nombre as producto_nombre " +
                     "FROM detalle_venta d " +
                     "LEFT JOIN productos p ON d.id_producto = p.id_producto " +
                     "WHERE d.id_producto = ? " +
                     "ORDER BY d.id_venta, d.id_detalle";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idProducto);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta detalle = mapResultSetToDetalleVenta(rs);
                    detalles.add(detalle);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar detalles de venta por ID de producto: " + idProducto, e);
        }
        
        return detalles;
    }
    
    /**
     * Guarda un nuevo detalle de venta en la base de datos.
     * @param detalle Detalle de venta a guardar
     * @return Detalle de venta guardado con su ID generado
     */
    @Override
    public DetalleVenta save(DetalleVenta detalle) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            return save(detalle, conn);
        } catch (SQLException e) {
            logger.error("Error al guardar detalle de venta", e);
            return detalle;
        }
    }
    
    /**
     * Guarda un nuevo detalle de venta en la base de datos utilizando una conexión existente.
     * @param detalle Detalle de venta a guardar
     * @param conn Conexión a la base de datos
     * @return Detalle de venta guardado con su ID generado
     * @throws SQLException Si ocurre un error al guardar el detalle
     */
    public DetalleVenta save(DetalleVenta detalle, Connection conn) throws SQLException {
        String sql = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, descuento_aplicado) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id_detalle";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, detalle.getIdVenta());
            stmt.setInt(2, detalle.getIdProducto());
            stmt.setInt(3, detalle.getCantidad());
            stmt.setDouble(4, detalle.getPrecioUnitario());
            stmt.setDouble(5, detalle.getDescuentoAplicado());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    detalle.setIdDetalle(rs.getInt(1));
                    logger.info("Detalle de venta guardado correctamente con ID: " + detalle.getIdDetalle());
                    return detalle;
                }
            }
        }
        
        return detalle;
    }
    
    /**
     * Actualiza un detalle de venta existente en la base de datos.
     * @param detalle Detalle de venta a actualizar
     * @return Detalle de venta actualizado
     */
    @Override
    public DetalleVenta update(DetalleVenta detalle) {
        String sql = "UPDATE detalle_venta SET id_venta = ?, id_producto = ?, cantidad = ?, " +
                     "precio_unitario = ?, descuento_aplicado = ? WHERE id_detalle = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, detalle.getIdVenta());
            stmt.setInt(2, detalle.getIdProducto());
            stmt.setInt(3, detalle.getCantidad());
            stmt.setDouble(4, detalle.getPrecioUnitario());
            stmt.setDouble(5, detalle.getDescuentoAplicado());
            stmt.setInt(6, detalle.getIdDetalle());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Detalle de venta actualizado correctamente con ID: " + detalle.getIdDetalle());
            } else {
                logger.warn("No se actualizó ningún detalle de venta con ID: " + detalle.getIdDetalle());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar detalle de venta con ID: " + detalle.getIdDetalle(), e);
        }
        
        return detalle;
    }
    
    /**
     * Elimina un detalle de venta de la base de datos por su ID.
     * @param id ID del detalle de venta a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            return delete(id, conn);
        } catch (SQLException e) {
            logger.error("Error al eliminar detalle de venta con ID: " + id, e);
            return false;
        }
    }
    
    /**
     * Elimina un detalle de venta de la base de datos por su ID utilizando una conexión existente.
     * @param id ID del detalle de venta a eliminar
     * @param conn Conexión a la base de datos
     * @return true si se eliminó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error al eliminar el detalle
     */
    public boolean delete(Integer id, Connection conn) throws SQLException {
        String sql = "DELETE FROM detalle_venta WHERE id_detalle = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Detalle de venta eliminado correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ningún detalle de venta con ID: " + id);
            }
        }
        
        return false;
    }
    
    /**
     * Elimina todos los detalles de una venta.
     * @param idVenta ID de la venta
     * @param conn Conexión a la base de datos
     * @return true si se eliminaron correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error al eliminar los detalles
     */
    public boolean deleteByVenta(Integer idVenta, Connection conn) throws SQLException {
        String sql = "DELETE FROM detalle_venta WHERE id_venta = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Se eliminaron " + rowsAffected + " detalles de la venta con ID: " + idVenta);
            return rowsAffected > 0;
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto DetalleVenta.
     * @param rs ResultSet con los datos del detalle de venta
     * @return Objeto DetalleVenta
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private DetalleVenta mapResultSetToDetalleVenta(ResultSet rs) throws SQLException {
        DetalleVenta detalle = new DetalleVenta(
            rs.getInt("id_detalle"),
            rs.getInt("id_venta"),
            rs.getInt("id_producto"),
            rs.getInt("cantidad"),
            rs.getDouble("precio_unitario"),
            rs.getDouble("descuento_aplicado")
        );
        
        detalle.setProductoNombre(rs.getString("producto_nombre"));
        return detalle;
    }
    
    /**
     * Obtiene la cantidad total vendida de un producto.
     * @param idProducto ID del producto
     * @return Cantidad total vendida
     */
    public int getCantidadTotalVendida(Integer idProducto) {
        String sql = "SELECT COALESCE(SUM(cantidad), 0) FROM detalle_venta WHERE id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idProducto);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener cantidad total vendida del producto: " + idProducto, e);
        }
        
        return 0;
    }
    
    /**
     * Obtiene los productos más vendidos.
     * @param limite Número máximo de productos a retornar
     * @return Lista de IDs de productos más vendidos con su cantidad
     */
    public List<Object[]> getProductosMasVendidos(int limite) {
        List<Object[]> productos = new ArrayList<>();
        String sql = "SELECT id_producto, SUM(cantidad) as total_vendido " +
                     "FROM detalle_venta " +
                     "GROUP BY id_producto " +
                     "ORDER BY total_vendido DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] producto = new Object[2];
                    producto[0] = rs.getInt("id_producto");
                    producto[1] = rs.getInt("total_vendido");
                    productos.add(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener productos más vendidos", e);
        }
        
        return productos;
    }
}