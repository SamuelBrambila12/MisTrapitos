package com.mistrapitos.dao;

import com.mistrapitos.models.Proveedor;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProveedorDao implements Dao<Proveedor, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ProveedorDao.class);

    @Override
    public List<Proveedor> findAll() {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre, contacto, direccion, telefono, correo, productos_vendidos " +
                "FROM proveedores ORDER BY nombre";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Proveedor proveedor = new Proveedor(
                        rs.getInt("id_proveedor"),
                        rs.getString("nombre"),
                        rs.getString("contacto"),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("correo"),
                        rs.getString("productos_vendidos")
                );
                proveedores.add(proveedor);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los proveedores", e);
        }
        return proveedores;
    }

    @Override
    public Optional<Proveedor> findById(Integer id) {
        String sql = "SELECT id_proveedor, nombre, contacto, direccion, telefono, correo, productos_vendidos " +
                "FROM proveedores WHERE id_proveedor = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Proveedor proveedor = new Proveedor(
                            rs.getInt("id_proveedor"),
                            rs.getString("nombre"),
                            rs.getString("contacto"),
                            rs.getString("direccion"),
                            rs.getString("telefono"),
                            rs.getString("correo"),
                            rs.getString("productos_vendidos")
                    );
                    return Optional.of(proveedor);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener proveedor por ID: " + id, e);
        }
        return Optional.empty();
    }

    public List<Proveedor> findByTermino(String termino) {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre, contacto, direccion, telefono, correo, productos_vendidos FROM proveedores " +
                "WHERE nombre ILIKE ? OR contacto ILIKE ? OR correo ILIKE ? " +
                "ORDER BY nombre";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchTerm = "%" + termino + "%";
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            stmt.setString(3, searchTerm);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Proveedor proveedor = new Proveedor(
                            rs.getInt("id_proveedor"),
                            rs.getString("nombre"),
                            rs.getString("contacto"),
                            rs.getString("direccion"),
                            rs.getString("telefono"),
                            rs.getString("correo"),
                            rs.getString("productos_vendidos")
                    );
                    proveedores.add(proveedor);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar proveedores por término: " + termino, e);
        }
        return proveedores;
    }

    @Override
    public Proveedor save(Proveedor proveedor) {
        String sql = "INSERT INTO proveedores (nombre, contacto, direccion, telefono, correo, productos_vendidos) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_proveedor";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getDireccion());
            stmt.setString(4, proveedor.getTelefono());
            stmt.setString(5, proveedor.getCorreo());
            stmt.setString(6, proveedor.getProductosVendidos());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    proveedor.setIdProveedor(rs.getInt(1));
                    logger.info("Proveedor guardado correctamente: " + proveedor.getNombre());
                    return proveedor;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar proveedor: " + proveedor.getNombre(), e);
        }
        return proveedor;
    }

    @Override
    public Proveedor update(Proveedor proveedor) {
        String sql = "UPDATE proveedores SET nombre = ?, contacto = ?, direccion = ?, telefono = ?, correo = ?, productos_vendidos = ? " +
                "WHERE id_proveedor = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getDireccion());
            stmt.setString(4, proveedor.getTelefono());
            stmt.setString(5, proveedor.getCorreo());
            stmt.setString(6, proveedor.getProductosVendidos());
            stmt.setInt(7, proveedor.getIdProveedor());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Proveedor actualizado correctamente: " + proveedor.getNombre());
            } else {
                logger.warn("No se actualizó ningún proveedor con ID: " + proveedor.getIdProveedor());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar proveedor: " + proveedor.getNombre(), e);
        }
        return proveedor;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM proveedores WHERE id_proveedor = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Proveedor eliminado correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ningún proveedor con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar proveedor con ID: " + id, e);
        }
        return false;
    }

    public List<Integer> getProductosAsociados(Integer idProveedor) {
        List<Integer> idsProductos = new ArrayList<>();
        String sql = "SELECT id_producto FROM productos WHERE proveedor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idProveedor);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    idsProductos.add(rs.getInt("id_producto"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener productos asociados al proveedor: " + idProveedor, e);
        }
        return idsProductos;
    }

    public boolean tieneProductosAsociados(Integer id) {
        String sql = "SELECT COUNT(*) FROM productos WHERE proveedor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar si el proveedor tiene productos asociados: " + id, e);
        }
        return false;
    }
}
