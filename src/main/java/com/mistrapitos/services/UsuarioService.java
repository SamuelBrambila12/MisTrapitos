package com.mistrapitos.services;

import com.mistrapitos.dao.UsuarioDao;
import com.mistrapitos.models.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de usuarios.
 */
public class UsuarioService {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    private final UsuarioDao usuarioDao;
    
    /**
     * Constructor.
     */
    public UsuarioService() {
        this.usuarioDao = new UsuarioDao();
    }
    
    /**
     * Obtiene todos los usuarios.
     * @return Lista de usuarios
     */
    public List<Usuario> obtenerTodos() {
        return usuarioDao.findAll();
    }
    
    /**
     * Obtiene un usuario por su ID.
     * @param id ID del usuario
     * @return Usuario encontrado o vacío si no existe
     */
    public Optional<Usuario> obtenerPorId(int id) {
        return usuarioDao.findById(id);
    }
    
    /**
     * Obtiene un usuario por su nombre de usuario.
     * @param nombreUsuario Nombre de usuario
     * @return Usuario encontrado o vacío si no existe
     */
    public Optional<Usuario> obtenerPorNombreUsuario(String nombreUsuario) {
        return usuarioDao.findByNombreUsuario(nombreUsuario);
    }
    
    /**
     * Guarda un nuevo usuario.
     * @param usuario Usuario a guardar
     * @return Usuario guardado con su ID generado
     */
    public Usuario guardar(Usuario usuario) {
        // Verificar si el nombre de usuario ya existe
        Optional<Usuario> existente = usuarioDao.findByNombreUsuario(usuario.getNombreUsuario());
        if (existente.isPresent()) {
            logger.warn("El nombre de usuario ya existe: " + usuario.getNombreUsuario());
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        
        return usuarioDao.save(usuario);
    }
    
    /**
     * Actualiza un usuario existente.
     * @param usuario Usuario a actualizar
     * @return Usuario actualizado
     */
    public Usuario actualizar(Usuario usuario) {
        // Verificar si el usuario existe
        Optional<Usuario> existente = usuarioDao.findById(usuario.getIdUsuario());
        if (!existente.isPresent()) {
            logger.warn("No se encontró el usuario con ID: " + usuario.getIdUsuario());
            throw new IllegalArgumentException("No se encontró el usuario");
        }
        
        // Verificar si el nombre de usuario ya existe (si se está cambiando)
        if (!existente.get().getNombreUsuario().equals(usuario.getNombreUsuario())) {
            Optional<Usuario> existenteNombre = usuarioDao.findByNombreUsuario(usuario.getNombreUsuario());
            if (existenteNombre.isPresent()) {
                logger.warn("El nombre de usuario ya existe: " + usuario.getNombreUsuario());
                throw new IllegalArgumentException("El nombre de usuario ya existe");
            }
        }
        
        return usuarioDao.update(usuario);
    }
    
    /**
     * Elimina un usuario.
     * @param id ID del usuario a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminar(int id) {
        // Verificar si el usuario existe
        Optional<Usuario> existente = usuarioDao.findById(id);
        if (!existente.isPresent()) {
            logger.warn("No se encontró el usuario con ID: " + id);
            throw new IllegalArgumentException("No se encontró el usuario");
        }
        
        return usuarioDao.delete(id);
    }
    
    /**
     * Autentica un usuario por su nombre de usuario y contraseña.
     * @param nombreUsuario Nombre de usuario
     * @param contrasena Contraseña
     * @return Usuario autenticado o vacío si las credenciales son incorrectas
     */
    public Optional<Usuario> autenticar(String nombreUsuario, String contrasena) {
        return usuarioDao.autenticar(nombreUsuario, contrasena);
    }
}