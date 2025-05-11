package com.mistrapitos.models;

import java.math.BigDecimal;

public class ResumenCategoria {
    private int idCategoria;
    private String nombreCategoria;
    private int totalProductos;
    private int stockTotal;
    private BigDecimal valorInventario;
    private int productosBajoStock;

    public ResumenCategoria(int idCategoria, String nombreCategoria, int totalProductos,
                            int stockTotal, BigDecimal valorInventario, int productosBajoStock) {
        this.idCategoria = idCategoria;
        this.nombreCategoria = nombreCategoria;
        this.totalProductos = totalProductos;
        this.stockTotal = stockTotal;
        this.valorInventario = valorInventario;
        this.productosBajoStock = productosBajoStock;
    }

    // Getters y Setters
    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public int getTotalProductos() { return totalProductos; }
    public void setTotalProductos(int totalProductos) { this.totalProductos = totalProductos; }

    public int getStockTotal() { return stockTotal; }
    public void setStockTotal(int stockTotal) { this.stockTotal = stockTotal; }

    public BigDecimal getValorInventario() { return valorInventario; }
    public void setValorInventario(BigDecimal valorInventario) { this.valorInventario = valorInventario; }

    public int getProductosBajoStock() { return productosBajoStock; }
    public void setProductosBajoStock(int productosBajoStock) { this.productosBajoStock = productosBajoStock; }
}