package com.mistrapitos.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo que representa una categoría de productos.
 */
public class Categoria {
    
    private final IntegerProperty idCategoria;
    private final StringProperty nombre;
    
    /**
     * Constructor por defecto.
     */
    public Categoria() {
        this(0, "");
    }
    
    /**
     * Constructor con parámetros.
     * @param idCategoria ID de la categoría
     * @param nombre Nombre de la categoría
     */
    public Categoria(int idCategoria, String nombre) {
        this.idCategoria = new SimpleIntegerProperty(idCategoria);
        this.nombre = new SimpleStringProperty(nombre);
    }
    
    // Getters y setters para idCategoria
    public int getIdCategoria() {
        return idCategoria.get();
    }
    
    public void setIdCategoria(int idCategoria) {
        this.idCategoria.set(idCategoria);
    }
    
    public IntegerProperty idCategoriaProperty() {
        return idCategoria;
    }
    
    // Getters y setters para nombre
    public String getNombre() {
        return nombre.get();
    }
    
    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }
    
    public StringProperty nombreProperty() {
        return nombre;
    }
    
    @Override
    public String toString() {
        return nombre.get();
    }
}