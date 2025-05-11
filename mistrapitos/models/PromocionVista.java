package com.mistrapitos.models;

import java.time.LocalDate;

public class PromocionVista {
    private int idProducto;
    private String nombreProducto;
    private Double descuentoDirecto; // Descuento directo de la tabla productos
    private Integer idPromocion;      // null si no hay promoción
    private Double porcentajePromocion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public PromocionVista() {}

    public PromocionVista(int idProducto, String nombreProducto, Double descuentoDirecto,
                          Integer idPromocion, Double porcentajePromocion,
                          LocalDate fechaInicio, LocalDate fechaFin) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.descuentoDirecto = descuentoDirecto;
        this.idPromocion = idPromocion;
        this.porcentajePromocion = porcentajePromocion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Double getDescuentoDirecto() {
        return descuentoDirecto;
    }

    public void setDescuentoDirecto(Double descuentoDirecto) {
        this.descuentoDirecto = descuentoDirecto;
    }

    public Integer getIdPromocion() {
        return idPromocion;
    }

    public void setIdPromocion(Integer idPromocion) {
        this.idPromocion = idPromocion;
    }

    public Double getPorcentajePromocion() {
        return porcentajePromocion;
    }

    public void setPorcentajePromocion(Double porcentajePromocion) {
        this.porcentajePromocion = porcentajePromocion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
}