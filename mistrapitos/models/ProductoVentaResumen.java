package com.mistrapitos.models;

/**
 * Modelo para mostrar el resumen de productos más vendidos.
 */
public class ProductoVentaResumen {
    private final String nombre;
    private final String categoria;
    private final int cantidadVendida;
    private final double totalVendido;

    public ProductoVentaResumen(String nombre, String categoria, int cantidadVendida, double totalVendido) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidadVendida = cantidadVendida;
        this.totalVendido = totalVendido;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getCantidadVendida() {
        return cantidadVendida;
    }

    public double getTotalVendido() {
        return totalVendido;
    }
}
