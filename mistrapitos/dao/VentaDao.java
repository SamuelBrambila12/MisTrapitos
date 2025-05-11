package com.mistrapitos.dao;

import com.mistrapitos.models.DetalleVenta;
import com.mistrapitos.models.Venta;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mistrapitos.models.ProductoVentaResumen;
import com.mistrapitos.models.MetodoPagoResumen;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Venta.
 */
public class VentaDao implements Dao<Venta, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(VentaDao.class);
    private final DetalleVentaDao detalleVentaDao = new DetalleVentaDao();
    private final ProductoDao productoDao = new ProductoDao();
    
    /**
     * Obtiene todas las ventas de la base de datos.
     * @return Lista de ventas
     */
    @Override
    public List<Venta> findAll() {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "ORDER BY v.fecha DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Venta venta = mapResultSetToVenta(rs);
                ventas.add(venta);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las ventas", e);
        }
        
        return ventas;
    }
    
    /**
     * Obtiene una venta por su ID.
     * @param id ID de la venta
     * @return Venta encontrada o vacío si no existe
     */
    @Override
    public Optional<Venta> findById(Integer id) {
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "WHERE v.id_venta = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    
                    // Cargar los detalles de la venta
                    List<DetalleVenta> detalles = detalleVentaDao.findByVenta(id);
                    for (DetalleVenta detalle : detalles) {
                        venta.addDetalle(detalle);
                    }
                    
                    return Optional.of(venta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener venta por ID: " + id, e);
        }
        
        return Optional.empty();
    }

    /**
     * Obtiene la lista de productos más vendidos en total histórico (todas las ventas).
     * @return Lista de productos más vendidos, ordenados por cantidad vendida descendente
     */
    public List<ProductoVentaResumen> obtenerProductosMasVendidos() {
        List<ProductoVentaResumen> lista = new ArrayList<>();
        String sql = "SELECT p.nombre, c.nombre AS categoria, SUM(dv.cantidad) AS cantidad_vendida, " +
                "SUM(dv.cantidad * dv.precio_unitario) AS total_vendido " +
                "FROM detalle_venta dv " +
                "JOIN productos p ON dv.id_producto = p.id_producto " +
                "JOIN categorias c ON p.id_categoria = c.id_categoria " +
                "GROUP BY p.nombre, c.nombre " +
                "HAVING SUM(dv.cantidad) >= 1 " +
                "ORDER BY cantidad_vendida DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProductoVentaResumen resumen = new ProductoVentaResumen(
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("cantidad_vendida"),
                            rs.getDouble("total_vendido")
                    );
                    lista.add(resumen);
                }
            }
        } catch (Exception e) {
            logger.error("Error al obtener productos más vendidos", e);
        }
        return lista;
    }

    /**
     * Cuenta ventas entre dos fechas.
     */
    public int contarVentasEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ventas WHERE fecha BETWEEN ? AND ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(desde));
            stmt.setDate(2, Date.valueOf(hasta));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Inserta una venta usando una conexión existente (para transacciones).
     * @param venta Venta a insertar
     * @param conn Conexión activa
     * @return ID generado de la venta, o -1 si falla
     */
    public int insertar(Venta venta, Connection conn) {
        String sql = "INSERT INTO ventas (id_cliente, fecha, metodo_pago, total) VALUES (?, ?, ?, ?) RETURNING id_venta";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (venta.getIdCliente() > 0) {
                stmt.setInt(1, venta.getIdCliente());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, venta.getMetodoPago());
            stmt.setDouble(4, venta.getTotal());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int idVenta = rs.getInt("id_venta");
                    venta.setIdVenta(idVenta);
                    logger.info("Venta insertada correctamente con ID: " + idVenta);
                    return idVenta;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al insertar venta", e);
        }
        return -1;
    }

    /**
     * Inserta un detalle de venta usando una conexión existente (para transacciones).
     * @param idVenta ID de la venta
     * @param idProducto ID del producto
     * @param cantidad Cantidad vendida
     * @param precioUnitario Precio unitario
     * @param descuentoAplicado Descuento aplicado
     * @param conn Conexión activa
     * @return true si se insertó correctamente
     */
    public boolean insertarDetalleVenta(int idVenta, int idProducto, int cantidad, double precioUnitario, double descuentoAplicado, Connection conn) {
        String sql = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, descuento_aplicado) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            stmt.setInt(2, idProducto);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, precioUnitario);
            stmt.setDouble(5, descuentoAplicado);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Detalle de venta insertado correctamente para venta ID: " + idVenta + ", producto ID: " + idProducto);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al insertar detalle de venta", e);
        }
        return false;
    }

    /**
     * Busca ventas por cliente.
     * @param idCliente ID del cliente
     * @return Lista de ventas del cliente
     */
    public List<Venta> findByCliente(Integer idCliente) {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "WHERE v.id_cliente = ? " +
                     "ORDER BY v.fecha DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idCliente);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    ventas.add(venta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar ventas por cliente: " + idCliente, e);
        }
        
        return ventas;
    }

    public List<MetodoPagoResumen> obtenerMetodosPagoMasUtilizados(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT metodo_pago, COUNT(*) AS veces " +
                "FROM ventas WHERE fecha BETWEEN ? AND ? " +
                "GROUP BY metodo_pago ORDER BY veces DESC";
        List<MetodoPagoResumen> lista = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(desde));
            stmt.setDate(2, Date.valueOf(hasta));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new MetodoPagoResumen(
                        rs.getString("metodo_pago"),
                        rs.getInt("veces")
                ));
            }
        }
        return lista;
    }

    /**
     * Busca ventas por fecha.
     * @param fecha Fecha de las ventas
     * @return Lista de ventas de la fecha especificada
     */
    public List<Venta> findByFecha(LocalDate fecha) {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "WHERE DATE(v.fecha) = ? " +
                     "ORDER BY v.fecha DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fecha));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    ventas.add(venta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar ventas por fecha: " + fecha, e);
        }
        
        return ventas;
    }
    
    /**
     * Busca ventas por rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de ventas en el rango de fechas especificado
     */
    public List<Venta> findByRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                     "ORDER BY v.fecha DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fechaInicio));
            stmt.setDate(2, Date.valueOf(fechaFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    ventas.add(venta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar ventas por rango de fechas: " + fechaInicio + " - " + fechaFin, e);
        }
        
        return ventas;
    }
    
    /**
     * Busca ventas por método de pago.
     * @param metodoPago Método de pago
     * @return Lista de ventas con el método de pago especificado
     */
    public List<Venta> findByMetodoPago(String metodoPago) {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_cliente, v.fecha, v.metodo_pago, v.total, c.nombre as cliente_nombre " +
                     "FROM ventas v " +
                     "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "WHERE v.metodo_pago = ? " +
                     "ORDER BY v.fecha DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, metodoPago);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    ventas.add(venta);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar ventas por método de pago: " + metodoPago, e);
        }
        
        return ventas;
    }
    
    /**
     * Guarda una nueva venta en la base de datos.
     * @param venta Venta a guardar
     * @return Venta guardada con su ID generado
     */
    @Override
    public Venta save(Venta venta) {
        String sql = "INSERT INTO ventas (id_cliente, fecha, metodo_pago, total) " +
                     "VALUES (?, ?, ?, ?) RETURNING id_venta";
        
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (venta.getIdCliente() > 0) {
                    stmt.setInt(1, venta.getIdCliente());
                } else {
                    stmt.setNull(1, Types.INTEGER);
                }
                
                stmt.setTimestamp(2, Timestamp.valueOf(venta.getFecha()));
                stmt.setString(3, venta.getMetodoPago());
                stmt.setDouble(4, venta.getTotal());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int idVenta = rs.getInt(1);
                        venta.setIdVenta(idVenta);
                        
                        // Guardar los detalles de la venta
                        for (DetalleVenta detalle : venta.getDetalles()) {
                            detalle.setIdVenta(idVenta);
                            detalleVentaDao.save(detalle, conn);
                            
                            // Actualizar el stock del producto
                            productoDao.updateStock(detalle.getIdProducto(), -detalle.getCantidad());
                        }
                        
                        conn.commit();
                        logger.info("Venta guardada correctamente con ID: " + idVenta);
                        return venta;
                    }
                }
            }
            
            conn.rollback();
        } catch (SQLException e) {
            logger.error("Error al guardar venta", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error al hacer rollback", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error al cerrar conexión", e);
                }
            }
        }
        
        return venta;
    }
    
    /**
     * Actualiza una venta existente en la base de datos.
     * @param venta Venta a actualizar
     * @return Venta actualizada
     */
    @Override
    public Venta update(Venta venta) {
        String sql = "UPDATE ventas SET id_cliente = ?, fecha = ?, metodo_pago = ?, total = ? WHERE id_venta = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (venta.getIdCliente() > 0) {
                    stmt.setInt(1, venta.getIdCliente());
                } else {
                    stmt.setNull(1, Types.INTEGER);
                }
                
                stmt.setTimestamp(2, Timestamp.valueOf(venta.getFecha()));
                stmt.setString(3, venta.getMetodoPago());
                stmt.setDouble(4, venta.getTotal());
                stmt.setInt(5, venta.getIdVenta());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Obtener los detalles actuales de la venta
                    List<DetalleVenta> detallesActuales = detalleVentaDao.findByVenta(venta.getIdVenta());
                    
                    // Eliminar los detalles actuales
                    for (DetalleVenta detalle : detallesActuales) {
                        // Restaurar el stock del producto
                        productoDao.updateStock(detalle.getIdProducto(), detalle.getCantidad());
                        detalleVentaDao.delete(detalle.getIdDetalle(), conn);
                    }
                    
                    // Guardar los nuevos detalles
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        detalle.setIdVenta(venta.getIdVenta());
                        detalleVentaDao.save(detalle, conn);
                        
                        // Actualizar el stock del producto
                        productoDao.updateStock(detalle.getIdProducto(), -detalle.getCantidad());
                    }
                    
                    conn.commit();
                    logger.info("Venta actualizada correctamente con ID: " + venta.getIdVenta());
                } else {
                    conn.rollback();
                    logger.warn("No se actualizó ninguna venta con ID: " + venta.getIdVenta());
                }
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar venta con ID: " + venta.getIdVenta(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error al hacer rollback", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error al cerrar conexión", e);
                }
            }
        }
        
        return venta;
    }
    
    /**
     * Elimina una venta de la base de datos por su ID.
     * @param id ID de la venta a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            // Obtener los detalles de la venta
            List<DetalleVenta> detalles = detalleVentaDao.findByVenta(id);
            
            // Restaurar el stock de los productos
            for (DetalleVenta detalle : detalles) {
                productoDao.updateStock(detalle.getIdProducto(), detalle.getCantidad());
            }
            
            // Eliminar los detalles de la venta
            detalleVentaDao.deleteByVenta(id, conn);
            
            // Eliminar la venta
            String sql = "DELETE FROM ventas WHERE id_venta = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    conn.commit();
                    logger.info("Venta eliminada correctamente con ID: " + id);
                    return true;
                } else {
                    conn.rollback();
                    logger.warn("No se eliminó ninguna venta con ID: " + id);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar venta con ID: " + id, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error al hacer rollback", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error al cerrar conexión", e);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Mapea un ResultSet a un objeto Venta.
     * @param rs ResultSet con los datos de la venta
     * @return Objeto Venta
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private Venta mapResultSetToVenta(ResultSet rs) throws SQLException {
        Venta venta = new Venta(
            rs.getInt("id_venta"),
            rs.getInt("id_cliente"),
            rs.getTimestamp("fecha").toLocalDateTime(),
            rs.getString("metodo_pago"),
            rs.getDouble("total")
        );
        
        venta.setClienteNombre(rs.getString("cliente_nombre"));
        return venta;
    }
    
    /**
     * Obtiene el total de ventas por día.
     * @param fecha Fecha para la cual se quiere obtener el total
     * @return Total de ventas del día
     */
    public double getTotalVentasPorDia(LocalDate fecha) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE DATE(fecha) = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fecha));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener total de ventas por día: " + fecha, e);
        }
        
        return 0.0;
    }
    
    /**
     * Obtiene el total de ventas por mes.
     * @param año Año
     * @param mes Mes (1-12)
     * @return Total de ventas del mes
     */
    public double getTotalVentasPorMes(int año, int mes) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE EXTRACT(YEAR FROM fecha) = ? AND EXTRACT(MONTH FROM fecha) = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, año);
            stmt.setInt(2, mes);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener total de ventas por mes: " + mes + "/" + año, e);
        }
        
        return 0.0;
    }
    
    /**
     * Obtiene el número de ventas por día.
     * @param fecha Fecha para la cual se quiere obtener el número de ventas
     * @return Número de ventas del día
     */
    public int getNumeroVentasPorDia(LocalDate fecha) {
        String sql = "SELECT COUNT(*) FROM ventas WHERE DATE(fecha) = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fecha));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener número de ventas por día: " + fecha, e);
        }
        
        return 0;
    }
}