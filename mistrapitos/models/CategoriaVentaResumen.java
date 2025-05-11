package com.mistrapitos.models;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CategoriaVentaResumen {
    private final SimpleStringProperty categoria;
    private final SimpleDoubleProperty total;

    public CategoriaVentaResumen(String categoria, double total) {
        this.categoria = new SimpleStringProperty(categoria);
        this.total     = new SimpleDoubleProperty(total);
    }
    public String getCategoria() { return categoria.get(); }
    public double getTotal()     { return total.get();     }
    public StringProperty categoriaProperty() { return categoria; }
    public DoubleProperty totalProperty()     { return total;     }
}

