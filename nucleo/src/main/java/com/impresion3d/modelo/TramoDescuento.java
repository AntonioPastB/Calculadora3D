package com.impresion3d.modelo;

/**
 * Descuento por volumen: a partir de {@code cantidadMinima} piezas se aplica
 * {@code porcentaje} de descuento sobre el precio de las piezas.
 */
public record TramoDescuento(int cantidadMinima, double porcentaje) {

    public TramoDescuento {
        if (cantidadMinima < 1) {
            throw new IllegalArgumentException("La cantidad mínima de un descuento debe ser al menos 1.");
        }
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100 %.");
        }
    }
}
