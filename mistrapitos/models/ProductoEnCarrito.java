package com.mistrapitos.models;

/**
 * Modelo para representar un producto en el carrito de ventas.
 */
public class ProductoEnCarrito {
    private int idProducto;
    private String nombre;
    private int cantidad;
    private double precioUnitario;
    private double descuento;

    public ProductoEnCarrito() {}

    public ProductoEnCarrito(Producto prod, int cantidad) {
        this.idProducto = prod.getIdProducto();
        this.nombre = prod.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = prod.getPrecio();
        this.descuento = prod.getDescuento();
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getSubtotal() {
        double precioConDesc = precioUnitario * (1 - descuento / 100.0);
        return cantidad * precioConDesc;
    }
}