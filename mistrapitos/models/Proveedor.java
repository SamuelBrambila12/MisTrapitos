package com.mistrapitos.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public class Proveedor {

    private final IntegerProperty idProveedor;
    private final StringProperty nombre;
    private final StringProperty contacto;
    private final StringProperty direccion;
    private final StringProperty telefono;
    private final StringProperty correo;
    // Nueva propiedad para los productos que vende (almacenados en formato CSV, por ejemplo)
    private final StringProperty productosVendidos;

    // Constructor por defecto
    public Proveedor() {
        this(0, "", "", "", "", "", "");
    }

    // Constructor con parámetros
    public Proveedor(int idProveedor, String nombre, String contacto,
                     String direccion, String telefono, String correo,
                     String productosVendidos) {
        this.idProveedor = new SimpleIntegerProperty(idProveedor);
        this.nombre = new SimpleStringProperty(nombre);
        this.contacto = new SimpleStringProperty(contacto);
        this.direccion = new SimpleStringProperty(direccion);
        this.telefono = new SimpleStringProperty(telefono);
        this.correo = new SimpleStringProperty(correo);
        this.productosVendidos = new SimpleStringProperty(productosVendidos);
    }

    // Getters y setters para idProveedor, nombre, contacto, direccion, teléfono y correo

    public int getIdProveedor() {
        return idProveedor.get();
    }
    public void setIdProveedor(int idProveedor) {
        this.idProveedor.set(idProveedor);
    }
    public IntegerProperty idProveedorProperty() {
        return idProveedor;
    }

    public String getNombre() {
        return nombre.get();
    }
    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }
    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getContacto() {
        return contacto.get();
    }
    public void setContacto(String contacto) {
        this.contacto.set(contacto);
    }
    public StringProperty contactoProperty() {
        return contacto;
    }

    public String getDireccion() {
        return direccion.get();
    }
    public void setDireccion(String direccion) {
        this.direccion.set(direccion);
    }
    public StringProperty direccionProperty() {
        return direccion;
    }

    public String getTelefono() {
        return telefono.get();
    }
    public void setTelefono(String telefono) {
        this.telefono.set(telefono);
    }
    public StringProperty telefonoProperty() {
        return telefono;
    }

    public String getCorreo() {
        return correo.get();
    }
    public void setCorreo(String correo) {
        this.correo.set(correo);
    }
    public StringProperty correoProperty() {
        return correo;
    }

    // Nuevos getter, setter y property para productosVendidos
    public String getProductosVendidos() {
        return productosVendidos.get();
    }
    public void setProductosVendidos(String productosVendidos) {
        this.productosVendidos.set(productosVendidos);
    }
    public StringProperty productosVendidosProperty() {
        return productosVendidos;
    }

    @Override
    public String toString() {
        return nombre.get();
    }
}
