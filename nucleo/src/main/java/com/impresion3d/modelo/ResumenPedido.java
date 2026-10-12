package com.impresion3d.modelo;

import java.util.List;

/**
 * Resultado del cálculo de un presupuesto completo, en euros.
 *
 * @param lineas            una línea por pieza, en el mismo orden que en el pedido
 * @param subtotalPiezas    suma de las piezas antes de descuentos
 * @param descuento         suma de los descuentos por volumen de todas las piezas
 * @param gastosAdicionales suma de los gastos del pedido
 * @param baseImponible     piezas con descuento más gastos, sin IVA
 * @param unidades          unidades totales de todas las piezas
 */
public record ResumenPedido(
        List<LineaPieza> lineas,
        double subtotalPiezas,
        double descuento,
        double gastosAdicionales,
        double baseImponible,
        double porcentajeIva,
        double iva,
        double total,
        int unidades) {

    /**
     * Cálculo de una pieza dentro del presupuesto.
     *
     * @param desglose       coste de una unidad, concepto por concepto
     * @param precioUnitario precio de venta de una unidad con sus gastos (sin IVA ni descuento)
     * @param subtotal       precio unitario por las unidades de la pieza
     * @param tramo          descuento por volumen aplicado a esta pieza, o {@code null}
     * @param descuento      importe descontado a esta pieza
     */
    public record LineaPieza(Pieza pieza, DesglosePrecio desglose, double precioUnitario, double subtotal,
            TramoDescuento tramo, double descuento) {

        public double porcentajeDescuento() {
            return tramo == null ? 0 : tramo.porcentaje();
        }

        /** Importe de la pieza con su descuento, sin IVA. */
        public double importe() {
            return subtotal - descuento;
        }
    }

    public ResumenPedido {
        lineas = Textos.copiaInmutable(lineas);
    }

    /** Lo que sale de media cada unidad con todo incluido (descuentos, gastos e IVA). */
    public double precioMedioPorUnidad() {
        return unidades == 0 ? 0 : total / unidades;
    }
}
