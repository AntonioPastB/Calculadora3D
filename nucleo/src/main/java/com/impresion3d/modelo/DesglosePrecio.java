package com.impresion3d.modelo;

/**
 * Resultado del cálculo de una pieza, con el importe de cada concepto en euros.
 *
 * @param costeTotal  coste de producción más el riesgo de fallos
 * @param gastosPieza gastos adicionales de la pieza (se suman sin margen)
 */
public record DesglosePrecio(
        double material,
        double electricidad,
        double amortizacion,
        double manoObra,
        double fallos,
        double costeTotal,
        double beneficio,
        double gastosPieza,
        double precioSinIva,
        double iva,
        double precioFinal) {
}
