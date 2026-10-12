package com.impresion3d.modelo;

/**
 * Parámetros de entrada necesarios para calcular el precio de una impresión.
 *
 * @param pesoGramos          peso de la pieza (incluidos soportes) en gramos
 * @param precioFilamentoKg   precio del filamento en €/kg
 * @param horasImpresion      duración de la impresión en horas
 * @param potenciaW           consumo medio de la impresora en vatios
 * @param precioKwh           precio de la electricidad en €/kWh
 * @param precioImpresora     coste de compra de la impresora en €
 * @param vidaUtilHoras       horas de vida útil estimadas de la impresora
 * @param mantenimientoHora   coste de mantenimiento/recambios en €/h
 * @param horasManoObra       tiempo de preparación y postprocesado en horas
 * @param precioHoraManoObra  coste de la mano de obra en €/h
 * @param porcentajeFallos    recargo por riesgo de fallos, en % del coste de producción (0-99)
 * @param margenBeneficio     margen de beneficio en %
 * @param iva                 IVA en %
 */
public record DatosImpresion(
        double pesoGramos,
        double precioFilamentoKg,
        double horasImpresion,
        double potenciaW,
        double precioKwh,
        double precioImpresora,
        double vidaUtilHoras,
        double mantenimientoHora,
        double horasManoObra,
        double precioHoraManoObra,
        double porcentajeFallos,
        double margenBeneficio,
        double iva) {

    public DatosImpresion {
        if (pesoGramos < 0 || precioFilamentoKg < 0 || horasImpresion < 0 || potenciaW < 0
                || precioKwh < 0 || precioImpresora < 0 || mantenimientoHora < 0
                || horasManoObra < 0 || precioHoraManoObra < 0 || margenBeneficio < 0 || iva < 0) {
            throw new IllegalArgumentException("Ningún valor puede ser negativo.");
        }
        if (vidaUtilHoras <= 0) {
            throw new IllegalArgumentException("La vida útil de la impresora debe ser mayor que 0.");
        }
        if (porcentajeFallos < 0 || porcentajeFallos >= 100) {
            throw new IllegalArgumentException("El porcentaje de fallos debe estar entre 0 y 99.");
        }
    }

    /** Número de valores que componen los datos (para guardarlos y cargarlos). */
    public static final int NUM_VALORES = 13;

    /** Devuelve los valores en el mismo orden que los parámetros del record. */
    public double[] comoArray() {
        return new double[] { pesoGramos, precioFilamentoKg, horasImpresion, potenciaW, precioKwh,
                precioImpresora, vidaUtilHoras, mantenimientoHora, horasManoObra,
                precioHoraManoObra, porcentajeFallos, margenBeneficio, iva };
    }

    /** Operación inversa de {@link #comoArray()}. */
    public static DatosImpresion desdeArray(double[] v) {
        if (v.length != NUM_VALORES) {
            throw new IllegalArgumentException("Se esperaban " + NUM_VALORES + " valores.");
        }
        return new DatosImpresion(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9],
                v[10], v[11], v[12]);
    }
}
