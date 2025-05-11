
package com.mistrapitos.models;

import javafx.beans.property.*;

import java.time.LocalDate;

/**
 * Modelo que representa una promoción.
 */
public class Promocion {

    private final IntegerProperty idPromocion;
    private final IntegerProperty idProducto;
    private final DoubleProperty porcentajeDescuento;
    private final ObjectProperty<LocalDate> fechaInicio;
    private final ObjectProperty<LocalDate> fechaFin;

    // Propiedades adicionales para la vista
    private final StringProperty productoNombre;

    /**
     * Constructor por defecto.
     */
    public Promocion() {
        this(0, 0, "", 0.0, LocalDate.now(), LocalDate.now().plusDays(7));
    }

    /**
     * Constructor con parámetros (sin nombre de producto).
     * @param idPromocion ID de la promoción
     * @param idProducto ID del producto
     * @param porcentajeDescuento Porcentaje de descuento
     * @param fechaInicio Fecha de inicio de la promoción
     * @param fechaFin Fecha de fin de la promoción
     */
    public Promocion(int idPromocion, int idProducto, double porcentajeDescuento,
                     LocalDate fechaInicio, LocalDate fechaFin) {
        this(idPromocion, idProducto, "", porcentajeDescuento, fechaInicio, fechaFin);
    }

    /**
     * Constructor completo con nombre de producto.
     * @param idPromocion ID de la promoción
     * @param idProducto ID del producto
     * @param productoNombre Nombre del producto
     * @param porcentajeDescuento Porcentaje de descuento
     * @param fechaInicio Fecha de inicio de la promoción
     * @param fechaFin Fecha de fin de la promoción
     */
    public Promocion(int idPromocion, int idProducto, String productoNombre, double porcentajeDescuento,
                     LocalDate fechaInicio, LocalDate fechaFin) {
        this.idPromocion = new SimpleIntegerProperty(idPromocion);
        this.idProducto = new SimpleIntegerProperty(idProducto);
        this.porcentajeDescuento = new SimpleDoubleProperty(porcentajeDescuento);
        this.fechaInicio = new SimpleObjectProperty<>(fechaInicio);
        this.fechaFin = new SimpleObjectProperty<>(fechaFin);
        this.productoNombre = new SimpleStringProperty(productoNombre != null ? productoNombre : "");
    }

    // Getters y setters para idPromocion
    public int getIdPromocion() {
        return idPromocion.get();
    }

    public void setIdPromocion(int idPromocion) {
        this.idPromocion.set(idPromocion);
    }

    public IntegerProperty idPromocionProperty() {
        return idPromocion;
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

    // Getters y setters para porcentajeDescuento
    public double getPorcentajeDescuento() {
        return porcentajeDescuento.get();
    }

    public void setPorcentajeDescuento(double porcentajeDescuento) {
        this.porcentajeDescuento.set(porcentajeDescuento);
    }

    public DoubleProperty porcentajeDescuentoProperty() {
        return porcentajeDescuento;
    }

    // Getters y setters para fechaInicio
    public LocalDate getFechaInicio() {
        return fechaInicio.get();
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio.set(fechaInicio);
    }

    public ObjectProperty<LocalDate> fechaInicioProperty() {
        return fechaInicio;
    }

    // Getters y setters para fechaFin
    public LocalDate getFechaFin() {
        return fechaFin.get();
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin.set(fechaFin);
    }

    public ObjectProperty<LocalDate> fechaFinProperty() {
        return fechaFin;
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
     * Verifica si la promoción está activa en la fecha actual.
     * @return true si la promoción está activa, false en caso contrario
     */
    public boolean isActiva() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicio.get()) && !hoy.isAfter(fechaFin.get());
    }
}
