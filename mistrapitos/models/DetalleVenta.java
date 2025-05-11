package com.mistrapitos.models;

import javafx.beans.property.*;

/**
 * Modelo que representa un detalle de venta.
 */
public class DetalleVenta {
    
    private final IntegerProperty idDetalle;
    private final IntegerProperty idVenta;
    private final IntegerProperty idProducto;
    private final IntegerProperty cantidad;
    private final DoubleProperty precioUnitario;
    private final DoubleProperty descuentoAplicado;
    
    // Propiedades adicionales para la vista
    private final StringProperty productoNombre;
    
    /**
     * Constructor por defecto.
     */
    public DetalleVenta() {
        this(0, 0, 0, 0, 0.0, 0.0);
    }
    
    /**
     * Constructor con parámetros.
     * @param idDetalle ID del detalle
     * @param idVenta ID de la venta
     * @param idProducto ID del producto
     * @param cantidad Cantidad de productos
     * @param precioUnitario Precio unitario del producto
     * @param descuentoAplicado Descuento aplicado al producto
     */
    public DetalleVenta(int idDetalle, int idVenta, int idProducto, int cantidad, 
                       double precioUnitario, double descuentoAplicado) {
        this.idDetalle = new SimpleIntegerProperty(idDetalle);
        this.idVenta = new SimpleIntegerProperty(idVenta);
        this.idProducto = new SimpleIntegerProperty(idProducto);
        this.cantidad = new SimpleIntegerProperty(cantidad);
        this.precioUnitario = new SimpleDoubleProperty(precioUnitario);
        this.descuentoAplicado = new SimpleDoubleProperty(descuentoAplicado);
        this.productoNombre = new SimpleStringProperty("");
    }
    
    // Getters y setters para idDetalle
    public int getIdDetalle() {
        return idDetalle.get();
    }
    
    public void setIdDetalle(int idDetalle) {
        this.idDetalle.set(idDetalle);
    }
    
    public IntegerProperty idDetalleProperty() {
        return idDetalle;
    }
    
    // Getters y setters para idVenta
    public int getIdVenta() {
        return idVenta.get();
    }
    
    public void setIdVenta(int idVenta) {
        this.idVenta.set(idVenta);
    }
    
    public IntegerProperty idVentaProperty() {
        return idVenta;
    }
    
    // Getters y setters para idProducto
    public int getIdProducto() {
        return idProducto.get();
    }
    
    public void setIdProducto(int idProducto) {
        this.idProducto.set(idProducto);
    }
    
    public IntegerProperty idProductoProperty() {
        return idProducto;
    }
    
    // Getters y setters para cantidad
    public int getCantidad() {
        return cantidad.get();
    }
    
    public void setCantidad(int cantidad) {
        this.cantidad.set(cantidad);
    }
    
    public IntegerProperty cantidadProperty() {
        return cantidad;
    }
    
    // Getters y setters para precioUnitario
    public double getPrecioUnitario() {
        return precioUnitario.get();
    }
    
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario.set(precioUnitario);
    }
    
    public DoubleProperty precioUnitarioProperty() {
        return precioUnitario;
    }
    
    // Getters y setters para descuentoAplicado
    public double getDescuentoAplicado() {
        return descuentoAplicado.get();
    }
    
    public void setDescuentoAplicado(double descuentoAplicado) {
        this.descuentoAplicado.set(descuentoAplicado);
    }
    
    public DoubleProperty descuentoAplicadoProperty() {
        return descuentoAplicado;
    }
    
    // Getters y setters para productoNombre
    public String getProductoNombre() {
        return productoNombre.get();
    }
    
    public void setProductoNombre(String productoNombre) {
        this.productoNombre.set(productoNombre);
    }
    
    public StringProperty productoNombreProperty() {
        return productoNombre;
    }
    
    /**
     * Calcula el precio con descuento.
     * @return Precio con descuento aplicado
     */
    public double getPrecioConDescuento() {
        return precioUnitario.get() * (1 - descuentoAplicado.get() / 100);
    }
    
    /**
     * Calcula el subtotal del detalle.
     * @return Subtotal del detalle
     */
    public double getSubtotal() {
        return getPrecioConDescuento() * cantidad.get();
    }
}