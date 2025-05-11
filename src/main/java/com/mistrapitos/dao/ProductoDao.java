package com.mistrapitos.dao;

import com.mistrapitos.models.Producto;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Producto.
 */
public class ProductoDao implements Dao<Producto, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductoDao.class);
    
    /**
     * Obtiene todos los productos de la base de datos.
     * @return Lista de productos
     */
    @Override
    public List<Producto> findAll() {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                     "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                     "ORDER BY p.nombre";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Producto producto = mapResultSetToProducto(rs);
                productos.add(producto);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los productos", e);
        }
        
        return productos;
    }
    
    /**
     * Obtiene un producto por su ID.
     * @param id ID del producto
     * @return Producto encontrado o vacío si no existe
     */
    @Override
    public Optional<Producto> findById(Integer id) {
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                     "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                     "WHERE p.id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Producto producto = mapResultSetToProducto(rs);
                    return Optional.of(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener producto por ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Busca productos por nombre o código de barras.
     * @param termino Término de búsqueda
     * @return Lista de productos que coinciden con el término
     */
    public List<Producto> findByNombreOrBarcode(String termino) {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                     "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                     "WHERE p.nombre ILIKE ? OR p.barcode ILIKE ? " +
                     "ORDER BY p.nombre";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchTerm = "%" + termino + "%";
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Producto producto = mapResultSetToProducto(rs);
                    productos.add(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar productos por término: " + termino, e);
        }
        
        return productos;
    }
    /**
     * Busca un producto por su código de barras.
     * @param barcode Código de barras
     * @return Producto encontrado o vacío si no existe
     */
    public Optional<Producto> buscarPorBarcode(String barcode) {
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                "FROM productos p " +
                "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                "WHERE p.barcode = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, barcode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Producto producto = mapResultSetToProducto(rs);
                    return Optional.of(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar producto por código de barras: " + barcode, e);
        }

        return Optional.empty();
    }

    /**
     * Actualiza el stock de un producto usando una conexión existente (para transacciones).
     * @param idProducto ID del producto
     * @param cantidad Cantidad a restar (debe ser negativa para ventas)
     * @param conn Conexión activa
     * @return true si se actualizó correctamente, false en caso contrario
     */
    public boolean actualizarStock(int idProducto, int cantidad, Connection conn) {
        String sql = "UPDATE productos SET stock = stock + ? WHERE id_producto = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cantidad);
            stmt.setInt(2, idProducto);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Stock actualizado correctamente (transacción) para producto ID: " + idProducto);
                return true;
            } else {
                logger.warn("No se actualizó el stock para ningún producto con ID: " + idProducto);
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar stock del producto ID: " + idProducto, e);
        }
        return false;
    }

    /**
     * Actualiza el descuento directo de un producto.
     * @param idProducto ID del producto
     * @param descuentoDirecto Nuevo descuento directo
     * @return true si se actualizó correctamente
     */
    public boolean actualizarDescuentoDirecto(int idProducto, double descuentoDirecto) {
        String sql = "UPDATE productos SET descuento = ? WHERE id_producto = ?";
        try (java.sql.Connection conn = com.mistrapitos.utils.DatabaseUtil.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, descuentoDirecto);
            stmt.setInt(2, idProducto);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca productos por categoría.
     * @param idCategoria ID de la categoría
     * @return Lista de productos de la categoría especificada
     */
    public List<Producto> findByCategoria(Integer idCategoria) {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                     "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                     "WHERE p.id_categoria = ? " +
                     "ORDER BY p.nombre";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idCategoria);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Producto producto = mapResultSetToProducto(rs);
                    productos.add(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar productos por categoría: " + idCategoria, e);
        }
        
        return productos;
    }
    
    /**
     * Busca productos con stock bajo.
     * @param stockMinimo Stock mínimo
     * @return Lista de productos con stock bajo
     */
    public List<Producto> findByStockBajo(int stockMinimo) {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descripcion, p.id_categoria, p.precio, " +
                     "p.stock, p.sizes, p.colors, p.descuento, p.barcode, c.nombre as categoria_nombre " +
                     "FROM productos p " +
                     "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria " +
                     "WHERE p.stock <= ? " +
                     "ORDER BY p.stock ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, stockMinimo);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Producto producto = mapResultSetToProducto(rs);
                    productos.add(producto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar productos con stock bajo: " + stockMinimo, e);
        }
        
        return productos;
    }
    
    /**
     * Guarda un nuevo producto en la base de datos.
     * @param producto Producto a guardar
     * @return Producto guardado con su ID generado
     */
    @Override
    public Producto save(Producto producto) {
        String sql = "INSERT INTO productos (nombre, descripcion, id_categoria, precio, stock, " +
                     "sizes, colors, descuento, barcode) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_producto";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            
            if (producto.getIdCategoria() > 0) {
                stmt.setInt(3, producto.getIdCategoria());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setDouble(4, producto.getPrecio());
            stmt.setInt(5, producto.getStock());
            stmt.setString(6, producto.getSizes());
            stmt.setString(7, producto.getColors());
            stmt.setDouble(8, producto.getDescuento());
            stmt.setString(9, producto.getBarcode());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    producto.setIdProducto(rs.getInt(1));
                    logger.info("Producto guardado correctamente: " + producto.getNombre());
                    return producto;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar producto: " + producto.getNombre(), e);
        }
        
        return producto;
    }
    
    /**
     * Actualiza un producto existente en la base de datos.
     * @param producto Producto a actualizar
     * @return Producto actualizado
     */
    @Override
    public Producto update(Producto producto) {
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, id_categoria = ?, " +
                     "precio = ?, stock = ?, sizes = ?, colors = ?, descuento = ?, barcode = ? " +
                     "WHERE id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            
            if (producto.getIdCategoria() > 0) {
                stmt.setInt(3, producto.getIdCategoria());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setDouble(4, producto.getPrecio());
            stmt.setInt(5, producto.getStock());
            stmt.setString(6, producto.getSizes());
            stmt.setString(7, producto.getColors());
            stmt.setDouble(8, producto.getDescuento());
            stmt.setString(9, producto.getBarcode());
            stmt.setInt(10, producto.getIdProducto());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Producto actualizado correctamente: " + producto.getNombre());
            } else {
                logger.warn("No se actualizó ningún producto con ID: " + producto.getIdProducto());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar producto: " + producto.getNombre(), e);
        }
        
        return producto;
    }
    
    /**
     * Actualiza el stock de un producto.
     * @param idProducto ID del producto
     * @param cantidad Cantidad a agregar o restar del stock
     * @return true si se actualizó correctamente, false en caso contrario
     */
    public boolean updateStock(int idProducto, int cantidad) {
        String sql = "UPDATE productos SET stock = stock + ? WHERE id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, cantidad);
            stmt.setInt(2, idProducto);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Stock actualizado correctamente para producto ID: " + idProducto);
                return true;
            } else {
                logger.warn("No se actualizó el stock para ningún producto con ID: " + idProducto);
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar stock del producto ID: " + idProducto, e);
        }
        
        return false;
    }
    
    /**
     * Elimina un producto de la base de datos por su ID.
     * @param id ID del producto a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM productos WHERE id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Producto eliminado correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ningún producto con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar producto con ID: " + id, e);
        }
        
        return false;
    }
    /**
     * Obtiene el producto con más unidades en stock.
     */
    /**
     * Obtiene el producto con más unidades en stock, incluyendo la categoría.
     */
    public Optional<Producto> findProductoConMasStock() {
        String sql = """
        SELECT 
          p.id_producto,
          p.nombre,
          p.descripcion,
          p.id_categoria,
          p.precio,
          p.stock,
          p.sizes,
          p.colors,
          p.descuento,
          p.barcode,
          c.nombre AS categoria_nombre
        FROM productos p
        LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
        ORDER BY p.stock DESC
        LIMIT 1
        """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar producto con más stock", e);
        }
        return Optional.empty();
    }

    /**
     * Filtra productos con precio > X y stock > 0.
     */
    public List<Producto> findByPrecioYStock(double precioMinimo) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE precio > ? AND stock > 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, precioMinimo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) lista.add(mapResultSetToProducto(rs));
        } catch (SQLException e) {
            logger.error("Error al filtrar productos por precio y stock", e);
        }
        return lista;
    }
    /**
     * Mapea un ResultSet a un objeto Producto.
     * @param rs ResultSet con los datos del producto
     * @return Objeto Producto
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto producto = new Producto(
            rs.getInt("id_producto"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getInt("id_categoria"),
            rs.getDouble("precio"),
            rs.getInt("stock"),
            rs.getString("sizes"),
            rs.getString("colors"),
            rs.getDouble("descuento"),
            rs.getString("barcode")
        );

        producto.setCategoriaNombre(rs.getString("categoria_nombre"));
        return producto;
    }
}