package com.mistrapitos.dao;

import com.mistrapitos.models.Promocion;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mistrapitos.models.PromocionVista;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Promocion.
 */
public class PromocionDao implements Dao<Promocion, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(PromocionDao.class);
    
    /**
     * Obtiene todas las promociones de la base de datos.
     * @return Lista de promociones
     */
    @Override
    public List<Promocion> findAll() {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                     "pr.fecha_fin, p.nombre as producto_nombre " +
                     "FROM promociones pr " +
                     "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                     "ORDER BY pr.fecha_inicio DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Promocion promocion = mapResultSetToPromocion(rs);
                promociones.add(promocion);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las promociones", e);
        }
        
        return promociones;
    }
    /**
     * Busca promociones por nombre de producto (parcial, insensible a mayúsculas).
     * @param nombreProducto Nombre (o parte) del producto
     * @return Lista de promociones que coinciden
     */
    public List<Promocion> findByNombreProducto(String nombreProducto) {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                "pr.fecha_fin, p.nombre as producto_nombre " +
                "FROM promociones pr " +
                "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                "WHERE LOWER(p.nombre) LIKE LOWER(?) " +
                "ORDER BY pr.fecha_inicio DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + nombreProducto + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Promocion promocion = mapResultSetToPromocion(rs);
                    promociones.add(promocion);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar promociones por nombre de producto: " + nombreProducto, e);
        }
        return promociones;
    }

    /**
     * Obtiene la lista combinada de productos con descuentos directos y promociones.
     */
    public List<PromocionVista> obtenerPromocionesYDescuentos() {
        List<PromocionVista> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descuento AS descuento_directo, " +
                "pr.id_promocion, pr.porcentaje_descuento, pr.fecha_inicio, pr.fecha_fin " +
                "FROM productos p " +
                "LEFT JOIN promociones pr ON pr.id_producto = p.id_producto";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                PromocionVista vista = new PromocionVista(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getObject("descuento_directo") != null ? rs.getDouble("descuento_directo") : 0.0,
                        rs.getObject("id_promocion") != null ? rs.getInt("id_promocion") : null,
                        rs.getObject("porcentaje_descuento") != null ? rs.getDouble("porcentaje_descuento") : null,
                        rs.getObject("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                        rs.getObject("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null
                );
                lista.add(vista);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener promociones y descuentos", e);
        }
        return lista;
    }

    /**
     * Busca productos/promociones por nombre de producto.
     */
    public List<PromocionVista> buscarPromocionesYDescuentosPorNombre(String nombre) {
        List<PromocionVista> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.nombre, p.descuento AS descuento_directo, " +
                "pr.id_promocion, pr.porcentaje_descuento, pr.fecha_inicio, pr.fecha_fin " +
                "FROM productos p " +
                "LEFT JOIN promociones pr ON pr.id_producto = p.id_producto " +
                "WHERE LOWER(p.nombre) LIKE LOWER(?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + nombre + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PromocionVista vista = new PromocionVista(
                            rs.getInt("id_producto"),
                            rs.getString("nombre"),
                            rs.getObject("descuento_directo") != null ? rs.getDouble("descuento_directo") : 0.0,
                            rs.getObject("id_promocion") != null ? rs.getInt("id_promocion") : null,
                            rs.getObject("porcentaje_descuento") != null ? rs.getDouble("porcentaje_descuento") : null,
                            rs.getObject("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                            rs.getObject("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null
                    );
                    lista.add(vista);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar promociones y descuentos por nombre", e);
        }
        return lista;
    }

    /**
     * Guarda o actualiza una promoción temporal para un producto.
     * Si idPromocion es null, inserta; si no, actualiza.
     */
    public boolean guardarPromocionTemporal(Integer idPromocion, int idProducto, double porcentaje, LocalDate inicio, LocalDate fin) {
        String sqlInsert = "INSERT INTO promociones (id_producto, porcentaje_descuento, fecha_inicio, fecha_fin) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE promociones SET porcentaje_descuento = ?, fecha_inicio = ?, fecha_fin = ? WHERE id_promocion = ?";
        try (Connection conn = DatabaseUtil.getConnection()) {
            if (idPromocion == null) {
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, idProducto);
                    stmt.setDouble(2, porcentaje);
                    stmt.setDate(3, Date.valueOf(inicio));
                    stmt.setDate(4, Date.valueOf(fin));
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setDouble(1, porcentaje);
                    stmt.setDate(2, Date.valueOf(inicio));
                    stmt.setDate(3, Date.valueOf(fin));
                    stmt.setInt(4, idPromocion);
                    stmt.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            logger.error("Error al guardar promoción temporal", e);
            return false;
        }
    }

    /**
     * Elimina una promoción temporal por su ID.
     */
    public boolean eliminarPromocion(int idPromocion) {
        String sql = "DELETE FROM promociones WHERE id_promocion = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPromocion);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error al eliminar promoción", e);
            return false;
        }
    }

    /**
     * Obtiene una promoción por su ID.
     * @param id ID de la promoción
     * @return Promoción encontrada o vacío si no existe
     */
    @Override
    public Optional<Promocion> findById(Integer id) {
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                     "pr.fecha_fin, p.nombre as producto_nombre " +
                     "FROM promociones pr " +
                     "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                     "WHERE pr.id_promocion = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Promocion promocion = mapResultSetToPromocion(rs);
                    return Optional.of(promocion);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener promoción por ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Busca promociones por producto.
     * @param idProducto ID del producto
     * @return Lista de promociones del producto
     */
    public List<Promocion> findByProducto(Integer idProducto) {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                     "pr.fecha_fin, p.nombre as producto_nombre " +
                     "FROM promociones pr " +
                     "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                     "WHERE pr.id_producto = ? " +
                     "ORDER BY pr.fecha_inicio DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idProducto);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Promocion promocion = mapResultSetToPromocion(rs);
                    promociones.add(promocion);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar promociones por producto: " + idProducto, e);
        }
        
        return promociones;
    }
    
    /**
     * Busca promociones activas en la fecha actual.
     * @return Lista de promociones activas
     */
    public List<Promocion> findActivas() {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                     "pr.fecha_fin, p.nombre as producto_nombre " +
                     "FROM promociones pr " +
                     "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                     "WHERE CURRENT_DATE BETWEEN pr.fecha_inicio AND pr.fecha_fin " +
                     "ORDER BY pr.porcentaje_descuento DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Promocion promocion = mapResultSetToPromocion(rs);
                promociones.add(promocion);
            }
        } catch (SQLException e) {
            logger.error("Error al buscar promociones activas", e);
        }
        
        return promociones;
    }
    
    /**
     * Busca promociones por rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de promociones en el rango de fechas
     */
    public List<Promocion> findByRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Promocion> promociones = new ArrayList<>();
        String sql = "SELECT pr.id_promocion, pr.id_producto, pr.porcentaje_descuento, pr.fecha_inicio, " +
                     "pr.fecha_fin, p.nombre as producto_nombre " +
                     "FROM promociones pr " +
                     "LEFT JOIN productos p ON pr.id_producto = p.id_producto " +
                     "WHERE (pr.fecha_inicio BETWEEN ? AND ?) OR (pr.fecha_fin BETWEEN ? AND ?) " +
                     "OR (pr.fecha_inicio <= ? AND pr.fecha_fin >= ?) " +
                     "ORDER BY pr.fecha_inicio";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fechaInicio));
            stmt.setDate(2, Date.valueOf(fechaFin));
            stmt.setDate(3, Date.valueOf(fechaInicio));
            stmt.setDate(4, Date.valueOf(fechaFin));
            stmt.setDate(5, Date.valueOf(fechaInicio));
            stmt.setDate(6, Date.valueOf(fechaFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Promocion promocion = mapResultSetToPromocion(rs);
                    promociones.add(promocion);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar promociones por rango de fechas: " + fechaInicio + " - " + fechaFin, e);
        }
        
        return promociones;
    }
    
    /**
     * Guarda una nueva promoción en la base de datos.
     * @param promocion Promoción a guardar
     * @return Promoción guardada con su ID generado
     */
    @Override
    public Promocion save(Promocion promocion) {
        String sql = "INSERT INTO promociones (id_producto, porcentaje_descuento, fecha_inicio, fecha_fin) " +
                     "VALUES (?, ?, ?, ?) RETURNING id_promocion";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, promocion.getIdProducto());
            stmt.setDouble(2, promocion.getPorcentajeDescuento());
            stmt.setDate(3, Date.valueOf(promocion.getFechaInicio()));
            stmt.setDate(4, Date.valueOf(promocion.getFechaFin()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    promocion.setIdPromocion(rs.getInt(1));
                    logger.info("Promoción guardada correctamente con ID: " + promocion.getIdPromocion());
                    
                    // Actualizar el descuento en el producto si la promoción está activa
                    if (promocion.isActiva()) {
                        actualizarDescuentoProducto(promocion.getIdProducto());
                    }
                    
                    return promocion;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar promoción para producto ID: " + promocion.getIdProducto(), e);
        }
        
        return promocion;
    }
    
    /**
     * Actualiza una promoción existente en la base de datos.
     * @param promocion Promoción a actualizar
     * @return Promoción actualizada
     */
    @Override
    public Promocion update(Promocion promocion) {
        String sql = "UPDATE promociones SET id_producto = ?, porcentaje_descuento = ?, " +
                     "fecha_inicio = ?, fecha_fin = ? WHERE id_promocion = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, promocion.getIdProducto());
            stmt.setDouble(2, promocion.getPorcentajeDescuento());
            stmt.setDate(3, Date.valueOf(promocion.getFechaInicio()));
            stmt.setDate(4, Date.valueOf(promocion.getFechaFin()));
            stmt.setInt(5, promocion.getIdPromocion());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Promoción actualizada correctamente con ID: " + promocion.getIdPromocion());
                
                // Actualizar el descuento en el producto
                actualizarDescuentoProducto(promocion.getIdProducto());
                
                return promocion;
            } else {
                logger.warn("No se actualizó ninguna promoción con ID: " + promocion.getIdPromocion());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar promoción con ID: " + promocion.getIdPromocion(), e);
        }
        
        return promocion;
    }
    
    /**
     * Elimina una promoción de la base de datos por su ID.
     * @param id ID de la promoción a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        // Primero obtenemos la promoción para saber qué producto actualizar después
        Optional<Promocion> promocionOpt = findById(id);
        if (!promocionOpt.isPresent()) {
            return false;
        }
        
        Promocion promocion = promocionOpt.get();
        String sql = "DELETE FROM promociones WHERE id_promocion = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Promoción eliminada correctamente con ID: " + id);
                
                // Actualizar el descuento en el producto
                actualizarDescuentoProducto(promocion.getIdProducto());
                
                return true;
            } else {
                logger.warn("No se eliminó ninguna promoción con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar promoción con ID: " + id, e);
        }
        
        return false;
    }
    
    /**
     * Actualiza el descuento en un producto basado en sus promociones activas.
     * @param idProducto ID del producto
     */
    private void actualizarDescuentoProducto(int idProducto) {
        // Buscar la promoción activa con mayor descuento para el producto
        String sqlPromocion = "SELECT MAX(porcentaje_descuento) FROM promociones " +
                              "WHERE id_producto = ? AND CURRENT_DATE BETWEEN fecha_inicio AND fecha_fin";
        
        // Actualizar el descuento del producto
        String sqlProducto = "UPDATE productos SET descuento = ? WHERE id_producto = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmtPromocion = conn.prepareStatement(sqlPromocion);
             PreparedStatement stmtProducto = conn.prepareStatement(sqlProducto)) {
            
            stmtPromocion.setInt(1, idProducto);
            
            double descuento = 0.0;
            try (ResultSet rs = stmtPromocion.executeQuery()) {
                if (rs.next() && rs.getObject(1) != null) {
                    descuento = rs.getDouble(1);
                }
            }
            
            stmtProducto.setDouble(1, descuento);
            stmtProducto.setInt(2, idProducto);
            stmtProducto.executeUpdate();
            
            logger.info("Actualizado descuento del producto ID: " + idProducto + " a " + descuento + "%");
        } catch (SQLException e) {
            logger.error("Error al actualizar descuento del producto ID: " + idProducto, e);
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto Promocion.
     * @param rs ResultSet con los datos de la promoción
     * @return Objeto Promocion
     * @throws SQLException Si ocurre un error al acceder a los datos
     */
    private Promocion mapResultSetToPromocion(ResultSet rs) throws SQLException {
        Promocion promocion = new Promocion(
            rs.getInt("id_promocion"),
            rs.getInt("id_producto"),
            rs.getDouble("porcentaje_descuento"),
            rs.getDate("fecha_inicio").toLocalDate(),
            rs.getDate("fecha_fin").toLocalDate()
        );
        
        promocion.setProductoNombre(rs.getString("producto_nombre"));
        return promocion;
    }
}