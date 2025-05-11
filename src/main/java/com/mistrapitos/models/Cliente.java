package com.mistrapitos.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cliente {

    private final IntegerProperty idCliente;
    private final StringProperty nombre;
    private final StringProperty direccion;
    private final StringProperty correo;
    private final StringProperty telefono;
    private final StringProperty ciudad;

    public Cliente() {
        this(0, "", "", "", "", "");
    }

    public Cliente(int idCliente, String nombre, String direccion, String correo, String telefono, String ciudad) {
        this.idCliente = new SimpleIntegerProperty(idCliente);
        this.nombre = new SimpleStringProperty(nombre);
        this.direccion = new SimpleStringProperty(direccion);
        this.correo = new SimpleStringProperty(correo);
        this.telefono = new SimpleStringProperty(telefono);
        this.ciudad = new SimpleStringProperty(ciudad);
    }

    // Propiedades para idCliente
    public IntegerProperty idClienteProperty() {
        return idCliente;
    }

    public int getIdCliente() {
        return idCliente.get();
    }

    public void setIdCliente(int idCliente) {
        this.idCliente.set(idCliente);
    }

    // Propiedades para nombre
    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    // Propiedades para direccion
    public StringProperty direccionProperty() {
        return direccion;
    }

    public String getDireccion() {
        return direccion.get();
    }

    public void setDireccion(String direccion) {
        this.direccion.set(direccion);
    }

    // Propiedades para correo
    public StringProperty correoProperty() {
        return correo;
    }

    public String getCorreo() {
        return correo.get();
    }

    public void setCorreo(String correo) {
        this.correo.set(correo);
    }

    // Propiedades para telefono
    public StringProperty telefonoProperty() {
        return telefono;
    }

    public String getTelefono() {
        return telefono.get();
    }

    public void setTelefono(String telefono) {
        this.telefono.set(telefono);
    }

    // Propiedades para ciudad
    public StringProperty ciudadProperty() {
        return ciudad;
    }

    public String getCiudad() {
        return ciudad.get();
    }

    public void setCiudad(String ciudad) {
        this.ciudad.set(ciudad);
    }
}