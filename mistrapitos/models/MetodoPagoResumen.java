package com.mistrapitos.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetodoPagoResumen {
    private final StringProperty metodoPago;
    private final IntegerProperty veces;

    public MetodoPagoResumen(String metodoPago, int veces) {
        this.metodoPago = new SimpleStringProperty(metodoPago);
        this.veces = new SimpleIntegerProperty(veces);
    }

    public String getMetodoPago() { return metodoPago.get(); }
    public void setMetodoPago(String m) { metodoPago.set(m); }
    public StringProperty metodoPagoProperty() { return metodoPago; }

    public int getVeces() { return veces.get(); }
    public void setVeces(int v) { veces.set(v); }
    public IntegerProperty vecesProperty() { return veces; }
}