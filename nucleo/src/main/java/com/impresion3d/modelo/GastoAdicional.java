package com.impresion3d.modelo;

/**
 * Gasto que se añade al pedido además del precio de las piezas (envío,
 * embalaje, diseño...). No lleva margen de beneficio, pero sí IVA.
 *
 * @param importe   importe en euros (sin IVA)
 * @param porUnidad {@code true} si se cobra por cada pieza; {@code false} si es una vez por pedido
 */
public record GastoAdicional(String concepto, double importe, boolean porUnidad) {

    public GastoAdicional {
        concepto = Textos.limpiar(concepto);
        if (importe < 0) {
            throw new IllegalArgumentException("Los gastos adicionales no pueden ser negativos.");
        }
    }

    /** Importe total de este gasto para un pedido de esa cantidad. */
    public double totalPara(int cantidad) {
        return porUnidad ? importe * cantidad : importe;
    }
}
