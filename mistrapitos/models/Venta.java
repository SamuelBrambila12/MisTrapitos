package com.mistrapitos.models;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo que representa una venta.
 */
public class Venta {
    
    private final IntegerProperty idVenta;
    private final IntegerProperty idCliente;
    private final ObjectProperty<LocalDateTime> fecha;
    private final StringProperty metodoPago;
    private final DoubleProperty total;
    
    // Propiedades adicionales para la vista
    private final StringProperty clienteNombre;
    private final ObservableList<DetalleVenta> detalles;
    
    /**
     * Constructor por defecto.
     */
    public Venta() {
        this(0, 0, LocalDateTime.now(), "", 0.0);
    }
    
    /**
     * Constructor con parámetros.
     * @param idVenta ID de la venta
     * @param idCliente ID del cliente
     * @param fecha Fecha de la venta
     * @param metodoPago Método de pago
     * @param total Total de la venta
     */
    public Venta(int idVenta, int idCliente, LocalDateTime fecha, String metodoPago, double total) {
        this.idVenta = new SimpleIntegerProperty(idVenta);
        this.idCliente = new SimpleIntegerProperty(idCliente);
        this.fecha = new SimpleObjectProperty<>(fecha);
        this.metodoPago = new SimpleStringProperty(metodoPago);
        this.total = new SimpleDoubleProperty(total);
        this.clienteNombre = new SimpleStringProperty("");
        this.detalles = FXCollections.observableArrayList();
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
    
    // Getters y setters para idCliente
    public int getIdCliente() {
        return idCliente.get();
    }
    
    public void setIdCliente(int idCliente) {
        this.idCliente.set(idCliente);
    }
    
    public IntegerProperty idClienteProperty() {
        return idCliente;
    }
    
    // Getters y setters para fecha
    public LocalDateTime getFecha() {
        return fecha.get();
    }
    
    public void setFecha(LocalDateTime fecha) {
        this.fecha.set(fecha);
    }
    
    public ObjectProperty<LocalDateTime> fechaProperty() {
        return fecha;
    }
    
    // Getters y setters para metodoPago
    public String getMetodoPago() {
        return metodoPago.get();
    }
    
    public void setMetodoPago(String metodoPago) {
        this.metodoPago.set(metodoPago);
    }
    
    public StringProperty metodoPagoProperty() {
        return metodoPago;
    }
    
    // Getters y setters para total
    public double getTotal() {
        return total.get();
    }
    
    public void setTotal(double total) {
        this.total.set(total);
    }
    
    public DoubleProperty totalProperty() {
        return total;
    }
    
    // Getters y setters para clienteNombre
    public String getClienteNombre() {
        return clienteNombre.get();
    }
    
    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre.set(clienteNombre);
    }
    
    public StringProperty clienteNombreProperty() {
        return clienteNombre;
    }
    
    // Getters y setters para detalles
    public ObservableList<DetalleVenta> getDetalles() {
        return detalles;
    }
    
    public void addDetalle(DetalleVenta detalle) {
        this.detalles.add(detalle);
        recalcularTotal();
    }
    
    public void removeDetalle(DetalleVenta detalle) {
        this.detalles.remove(detalle);
        recalcularTotal();
    }
    
    public void clearDetalles() {
        this.detalles.clear();
        recalcularTotal();
    }
    
    /**
     * Recalcula el total de la venta basado en los detalles.
     */
    private void recalcularTotal() {
        double nuevoTotal = 0.0;
        for (DetalleVenta detalle : detalles) {
            nuevoTotal += detalle.getSubtotal();
        }
        setTotal(nuevoTotal);
    }
}