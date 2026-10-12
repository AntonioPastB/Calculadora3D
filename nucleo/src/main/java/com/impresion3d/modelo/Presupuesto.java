package com.impresion3d.modelo;

import java.time.LocalDateTime;

/**
 * Presupuesto guardado en el historial.
 *
 * @param nombre nombre opcional (cadena vacía si no tiene)
 * @param taller datos comunes con los que se calculó
 * @param pedido piezas, gastos y descuentos
 * @param total  total con IVA en el momento de guardarlo
 */
public record Presupuesto(
        String nombre,
        LocalDateTime fecha,
        Taller taller,
        Pedido pedido,
        double total) {

    public Presupuesto {
        nombre = Textos.limpiar(nombre);
    }

    /** Nombre para mostrar en pantalla. */
    public String nombreVisible() {
        return nombre.isEmpty() ? "Sin nombre" : nombre;
    }

    public int unidades() {
        return pedido.unidades();
    }

    /** Lo que sale de media cada unidad con todo incluido. */
    public double precioMedioPorUnidad() {
        return total / Math.max(1, unidades());
    }

    public Presupuesto conNombre(String nuevoNombre) {
        return new Presupuesto(nuevoNombre, fecha, taller, pedido, total);
    }
}
