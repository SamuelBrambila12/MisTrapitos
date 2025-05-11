package com.mistrapitos.models;

import javafx.beans.property.*;

/**
 * Modelo que representa un producto.
 */
public class Producto {
    
    private final IntegerProperty idProducto;
    private final StringProperty nombre;
    private final StringProperty descripcion;
    private final IntegerProperty idCategoria;
    private final DoubleProperty precio;
    private final IntegerProperty stock;
    private final StringProperty sizes;
    private final StringProperty colors;
    private final DoubleProperty descuento;
    private final StringProperty barcode;
    
    // Propiedades adicionales para la vista
    private final StringProperty categoriaNombre;
    
    /**
     * Constructor por defecto.
     */
    public Producto() {
        this(0, "", "", 0, 0.0, 0, "", "", 0.0, "");
    }
    
    /**
     * Constructor con parámetros.
     * @param idProducto ID del producto
     * @param nombre Nombre del producto
     * @param descripcion Descripción del producto
     * @param idCategoria ID de la categoría
     * @param precio Precio del producto
     * @param stock Stock del producto
     * @param sizes Tallas disponibles
     * @param colors Colores disponibles
     * @param descuento Descuento aplicado
     * @param barcode Código de barras
     */
    public Producto(int idProducto, String nombre, String descripcion, int idCategoria, 
                   double precio, int stock, String sizes, String colors, 
                   double descuento, String barcode) {
        this.idProducto = new SimpleIntegerProperty(idProducto);
        this.nombre = new SimpleStringProperty(nombre);
        this.descripcion = new SimpleStringProperty(descripcion);
        this.idCategoria = new SimpleIntegerProperty(idCategoria);
        this.precio = new SimpleDoubleProperty(precio);
        this.stock = new SimpleIntegerProperty(stock);
        this.sizes = new SimpleStringProperty(sizes);
        this.colors = new SimpleStringProperty(colors);
        this.descuento = new SimpleDoubleProperty(descuento);
        this.barcode = new SimpleStringProperty(barcode);
        this.categoriaNombre = new SimpleStringProperty("");
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
    
    // Getters y setters para descripcion
    public String getDescripcion() {
        return descripcion.get();
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }
    
    public StringProperty descripcionProperty() {
        return descripcion;
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
    
    // Getters y setters para precio
    public double getPrecio() {
        return precio.get();
    }
    
    public void setPrecio(double precio) {
        this.precio.set(precio);
    }
    
    public DoubleProperty precioProperty() {
        return precio;
    }
    
    // Getters y setters para stock
    public int getStock() {
        return stock.get();
    }
    
    public void setStock(int stock) {
        this.stock.set(stock);
    }
    
    public IntegerProperty stockProperty() {
        return stock;
    }
    
    // Getters y setters para sizes
    public String getSizes() {
        return sizes.get();
    }
    
    public void setSizes(String sizes) {
        this.sizes.set(sizes);
    }
    
    public StringProperty sizesProperty() {
        return sizes;
    }
    
    // Getters y setters para colors
    public String getColors() {
        return colors.get();
    }
    
    public void setColors(String colors) {
        this.colors.set(colors);
    }
    
    public StringProperty colorsProperty() {
        return colors;
    }
    
    // Getters y setters para descuento
    public double getDescuento() {
        return descuento.get();
    }
    
    public void setDescuento(double descuento) {
        this.descuento.set(descuento);
    }
    
    public DoubleProperty descuentoProperty() {
        return descuento;
    }
    
    // Getters y setters para barcode
    public String getBarcode() {
        return barcode.get();
    }
    
    public void setBarcode(String barcode) {
        this.barcode.set(barcode);
    }
    
    public StringProperty barcodeProperty() {
        return barcode;
    }
    
    // Getters y setters para categoriaNombre
    public String getCategoriaNombre() {
        return categoriaNombre.get();
    }
    
    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre.set(categoriaNombre);
    }
    
    public StringProperty categoriaNombreProperty() {
        return categoriaNombre;
    }
    
    /**
     * Calcula el precio con descuento.
     * @return Precio con descuento aplicado
     */
    public double getPrecioConDescuento() {
        return precio.get() * (1 - descuento.get() / 100);
    }
    
    @Override
    public String toString() {
        return nombre.get();
    }
}