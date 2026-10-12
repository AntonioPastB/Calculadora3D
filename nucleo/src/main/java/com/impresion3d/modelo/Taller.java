package com.impresion3d.modelo;

/**
 * Datos comunes a todas las piezas de un presupuesto: la impresora, la luz,
 * la mano de obra y las condiciones de venta.
 *
 * @param potenciaW          consumo medio de la impresora en vatios
 * @param precioKwh          precio de la electricidad en €/kWh
 * @param precioImpresora    coste de compra de la impresora en €
 * @param vidaUtilHoras      horas de vida útil estimadas de la impresora
 * @param mantenimientoHora  coste de mantenimiento/recambios en €/h
 * @param precioHoraManoObra coste de la mano de obra en €/h
 * @param porcentajeFallos   recargo por riesgo de fallos, en % del coste de producción (0-99)
 * @param margenBeneficio    margen de beneficio en %
 * @param iva                IVA en %
 */
public record Taller(
        double potenciaW,
        double precioKwh,
        double precioImpresora,
        double vidaUtilHoras,
        double mantenimientoHora,
        double precioHoraManoObra,
        double porcentajeFallos,
        double margenBeneficio,
        double iva) {

    /** Valores con los que arranca la aplicación la primera vez. */
    public static final Taller POR_DEFECTO = new Taller(150, 0.18, 400, 5000, 0.05, 15, 10, 30, 21);

    /** Número de valores que componen los datos (para guardarlos y cargarlos). */
    public static final int NUM_VALORES = 9;

    public Taller {
        if (potenciaW < 0 || precioKwh < 0 || precioImpresora < 0 || mantenimientoHora < 0
                || precioHoraManoObra < 0 || margenBeneficio < 0 || iva < 0) {
            throw new IllegalArgumentException("Ningún valor puede ser negativo.");
        }
        if (vidaUtilHoras <= 0) {
            throw new IllegalArgumentException("La vida útil de la impresora debe ser mayor que 0.");
        }
        if (porcentajeFallos < 0 || porcentajeFallos >= 100) {
            throw new IllegalArgumentException("El riesgo de fallos debe estar entre 0 y 99 %.");
        }
    }

    public double[] comoArray() {
        return new double[] { potenciaW, precioKwh, precioImpresora, vidaUtilHoras, mantenimientoHora,
                precioHoraManoObra, porcentajeFallos, margenBeneficio, iva };
    }

    public static Taller desdeArray(double[] v) {
        if (v.length != NUM_VALORES) {
            throw new IllegalArgumentException("Se esperaban " + NUM_VALORES + " valores.");
        }
        return new Taller(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
    }

    /** Extrae la parte común de unos datos de impresión antiguos (de una sola pieza). */
    public static Taller de(DatosImpresion d) {
        return new Taller(d.potenciaW(), d.precioKwh(), d.precioImpresora(), d.vidaUtilHoras(),
                d.mantenimientoHora(), d.precioHoraManoObra(), d.porcentajeFallos(), d.margenBeneficio(), d.iva());
    }
}
