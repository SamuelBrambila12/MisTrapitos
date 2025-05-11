package com.mistrapitos.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo que representa un usuario del sistema.
 */
public class Usuario {
    
    private final IntegerProperty idUsuario;
    private final StringProperty nombreUsuario;
    private final StringProperty contrasena;
    private final StringProperty rol;
    
    /**
     * Constructor por defecto.
     */
    public Usuario() {
        this(0, "", "", "");
    }
    
    /**
     * Constructor con parámetros.
     * @param idUsuario ID del usuario
     * @param nombreUsuario Nombre de usuario
     * @param contrasena Contraseña del usuario
     * @param rol Rol del usuario (ADMIN, VENDEDOR, etc.)
     */
    public Usuario(int idUsuario, String nombreUsuario, String contrasena, String rol) {
        this.idUsuario = new SimpleIntegerProperty(idUsuario);
        this.nombreUsuario = new SimpleStringProperty(nombreUsuario);
        this.contrasena = new SimpleStringProperty(contrasena);
        this.rol = new SimpleStringProperty(rol);
    }
    
    // Getters y setters para idUsuario
    public int getIdUsuario() {
        return idUsuario.get();
    }
    
    public void setIdUsuario(int idUsuario) {
        this.idUsuario.set(idUsuario);
    }
    
    public IntegerProperty idUsuarioProperty() {
        return idUsuario;
    }
    
    // Getters y setters para nombreUsuario
    public String getNombreUsuario() {
        return nombreUsuario.get();
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario.set(nombreUsuario);
    }
    
    public StringProperty nombreUsuarioProperty() {
        return nombreUsuario;
    }
    
    // Getters y setters para contrasena
    public String getContrasena() {
        return contrasena.get();
    }
    
    public void setContrasena(String contrasena) {
        this.contrasena.set(contrasena);
    }
    
    public StringProperty contrasenaProperty() {
        return contrasena;
    }
    
    // Getters y setters para rol
    public String getRol() {
        return rol.get();
    }
    
    public void setRol(String rol) {
        this.rol.set(rol);
    }
    
    public StringProperty rolProperty() {
        return rol;
    }
    
    /**
     * Verifica si el usuario tiene un rol específico.
     * @param rolVerificar Rol a verificar
     * @return true si el usuario tiene el rol especificado, false en caso contrario
     */
    public boolean tieneRol(String rolVerificar) {
        return rol.get().equals(rolVerificar);
    }
    
    /**
     * Verifica si el usuario es administrador.
     * @return true si el usuario es administrador, false en caso contrario
     */
    public boolean isAdmin() {
        return tieneRol("ADMIN");
    }
    
    @Override
    public String toString() {
        return nombreUsuario.get() + " (" + rol.get() + ")";
    }
}