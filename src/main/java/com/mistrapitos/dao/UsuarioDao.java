package com.mistrapitos.dao;

import com.mistrapitos.models.Usuario;
import com.mistrapitos.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para la entidad Usuario.
 */
public class UsuarioDao implements Dao<Usuario, Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioDao.class);
    
    /**
     * Obtiene todos los usuarios de la base de datos.
     * @return Lista de usuarios
     */
    @Override
    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre_usuario, contrasena, rol FROM usuarios";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Usuario usuario = new Usuario(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario"),
                    rs.getString("contrasena"),
                    rs.getString("rol")
                );
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener todos los usuarios", e);
        }
        
        return usuarios;
    }
    
    /**
     * Obtiene un usuario por su ID.
     * @param id ID del usuario
     * @return Usuario encontrado o vacío si no existe
     */
    @Override
    public Optional<Usuario> findById(Integer id) {
        String sql = "SELECT id_usuario, nombre_usuario, contrasena, rol FROM usuarios WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("contrasena"),
                        rs.getString("rol")
                    );
                    return Optional.of(usuario);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener usuario por ID: " + id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Busca un usuario por su nombre de usuario.
     * @param nombreUsuario Nombre de usuario
     * @return Usuario encontrado o vacío si no existe
     */
    public Optional<Usuario> findByNombreUsuario(String nombreUsuario) {
        String sql = "SELECT id_usuario, nombre_usuario, contrasena, rol FROM usuarios WHERE nombre_usuario = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nombreUsuario);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("contrasena"),
                        rs.getString("rol")
                    );
                    return Optional.of(usuario);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener usuario por nombre: " + nombreUsuario, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Guarda un nuevo usuario en la base de datos.
     * @param usuario Usuario a guardar
     * @return Usuario guardado con su ID generado
     */
    @Override
    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre_usuario, contrasena, rol) VALUES (?, ?, ?) RETURNING id_usuario";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuario.getNombreUsuario());
            stmt.setString(2, usuario.getContrasena());
            stmt.setString(3, usuario.getRol());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario.setIdUsuario(rs.getInt(1));
                    logger.info("Usuario guardado correctamente: " + usuario.getNombreUsuario());
                    return usuario;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al guardar usuario: " + usuario.getNombreUsuario(), e);
        }
        
        return usuario;
    }
    
    /**
     * Actualiza un usuario existente en la base de datos.
     * @param usuario Usuario a actualizar
     * @return Usuario actualizado
     */
    @Override
    public Usuario update(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre_usuario = ?, contrasena = ?, rol = ? WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuario.getNombreUsuario());
            stmt.setString(2, usuario.getContrasena());
            stmt.setString(3, usuario.getRol());
            stmt.setInt(4, usuario.getIdUsuario());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Usuario actualizado correctamente: " + usuario.getNombreUsuario());
            } else {
                logger.warn("No se actualizó ningún usuario con ID: " + usuario.getIdUsuario());
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar usuario: " + usuario.getNombreUsuario(), e);
        }
        
        return usuario;
    }
    
    /**
     * Elimina un usuario de la base de datos por su ID.
     * @param id ID del usuario a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Usuario eliminado correctamente con ID: " + id);
                return true;
            } else {
                logger.warn("No se eliminó ningún usuario con ID: " + id);
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar usuario con ID: " + id, e);
        }
        
        return false;
    }
    
    /**
     * Autentica un usuario por su nombre de usuario y contraseña.
     * @param nombreUsuario Nombre de usuario
     * @param contrasena Contraseña
     * @return Usuario autenticado o vacío si las credenciales son incorrectas
     */
    public Optional<Usuario> autenticar(String nombreUsuario, String contrasena) {
        String sql = "SELECT id_usuario, nombre_usuario, contrasena, rol FROM usuarios WHERE nombre_usuario = ? AND contrasena = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nombreUsuario);
            stmt.setString(2, contrasena);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("contrasena"),
                        rs.getString("rol")
                    );
                    logger.info("Usuario autenticado correctamente: " + nombreUsuario);
                    return Optional.of(usuario);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al autenticar usuario: " + nombreUsuario, e);
        }
        
        logger.warn("Intento de autenticación fallido para el usuario: " + nombreUsuario);
        return Optional.empty();
    }
}