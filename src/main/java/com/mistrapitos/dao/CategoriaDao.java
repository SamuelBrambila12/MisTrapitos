package com.mistrapitos.dao;

import com.mistrapitos.models.Categoria;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Categoria.
 */
public class CategoriaDao implements Dao<Categoria, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoriaDao.class);
    
    /**
     * Obtiene todas las categorías de la base de datos.
     * @return Lista de categorías
     */
    @Override
    public List<Categoria> findAll() {
        List<Categoria> categorias = new ArrayList<>();
        String sql = "SELECT id_categoria, nombre FROM categorias ORDER BY nombre";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Categoria categoria = new Categoria(
                    rs.getInt("id_categoria"),
                    rs.getString("nombre")
                );
                categorias.add(categoria);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todas las categorías", e);
        }
        
        return categorias;
    }

    /**
     * Inserta una nueva categoría en la base de datos.
     * @param categoria Categoría a guardar
     * @return Categoría guardada con su ID generado, o null si falla
     */
    public Categoria save(Categoria categoria) {
        String sql = "INSERT INTO categorias (nombre) VALUES (?) RETURNING id_categoria";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria.getNombre());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    categoria.setIdCategoria(rs.getInt(1));
                    logger.info("Categoría guardada correctamente: " + categoria.getNombre());
                    return categoria;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar categoría: " + categoria.getNombre(), e);
        }
        return null;
    }
    /**
     * Obtiene una categoría por su ID.
     * @param id ID de la categoría
     * @return Categoría encontrada o vacío si no existe
     */
    @Override
    public Optional<Categoria> findById(Integer id) {
        String sql = "SELECT id_categoria, nombre FROM categorias WHERE id_categoria = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Categoria categoria = new Categoria(
                        rs.getInt("id_categoria"),
                        rs.getString("nombre")
                    );
                    return Optional.of(categoria);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener categoría por ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Busca una categoría por su nombre.
     * @param nombre Nombre de la categoría
     * @return Categoría encontrada o vacío si no existe
     */
    public Optional<Categoria> findByNombre(String nombre) {
        String sql = "SELECT id_categoria, nombre FROM categorias WHERE nombre = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nombre);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Categoria categoria = new Categoria(
                        rs.getInt("id_categoria"),
                        rs.getString("nombre")
                    );
                    return Optional.of(categoria);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener categoría por nombre: " + nombre, e);
        }
        
        return Optional.empty();
    }
    

    
    /**
     * Actualiza una categoría existente en la base de datos.
     * @param categoria Categoría a actualizar
     * @return Categoría actualizada
     */
    @Override
    public Categoria update(Categoria categoria) {
        String sql = "UPDATE categorias SET nombre = ? WHERE id_categoria = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, categoria.getNombre());
            stmt.setInt(2, categoria.getIdCategoria());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Categoría actualizada correctamente: " + categoria.getNombre());
            } else {
                logger.warn("No se actualizó ninguna categoría con ID: " + categoria.getIdCategoria());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar categoría: " + categoria.getNombre(), e);
        }
        
        return categoria;
    }
    
    /**
     * Elimina una categoría de la base de datos por su ID.
     * @param id ID de la categoría a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM categorias WHERE id_categoria = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Categoría eliminada correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ninguna categoría con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar categoría con ID: " + id, e);
        }
        
        return false;
    }
    
    /**
     * Verifica si una categoría está siendo utilizada por algún producto.
     * @param id ID de la categoría
     * @return true si la categoría está siendo utilizada, false en caso contrario
     */
    public boolean isInUse(Integer id) {
        String sql = "SELECT COUNT(*) FROM productos WHERE id_categoria = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar si la categoría está en uso: " + id, e);
        }
        
        return false;
    }
}