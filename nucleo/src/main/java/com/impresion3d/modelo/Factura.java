package com.impresion3d.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Factura emitida. Guarda una copia de los datos del emisor y del cliente tal
 * y como estaban al emitirla, para que no cambie aunque luego se modifiquen.
 *
 * @param numero            número correlativo por año: "2026-0001"
 * @param taller            datos comunes con los que se calculó
 * @param pedido            piezas, gastos y descuentos
 * @param total             total con IVA en el momento de emitirla
 * @param presupuestoOrigen nombre del presupuesto del que procede (puede estar vacío)
 */
public record Factura(
        String numero,
        LocalDate fecha,
        Emisor emisor,
        Cliente cliente,
        Taller taller,
        Pedido pedido,
        double total,
        String presupuestoOrigen) {

    public Factura {
        presupuestoOrigen = Textos.limpiar(presupuestoOrigen);
    }

    public ResumenPedido resumen() {
        return CalculadoraPrecio.calcularPedido(taller, pedido);
    }

    /**
     * Siguiente número para una factura con esa fecha: el mayor del mismo año
     * más uno ("2026-0001", "2026-0002"...). Cada año empieza de nuevo en 0001.
     */
    public static String siguienteNumero(List<Factura> emitidas, LocalDate fecha) {
        String prefijo = fecha.getYear() + "-";
        int mayor = 0;
        for (Factura f : emitidas) {
            if (f.numero().startsWith(prefijo)) {
                try {
                    mayor = Math.max(mayor, Integer.parseInt(f.numero().substring(prefijo.length())));
                } catch (NumberFormatException e) {
                    // Número con otro formato: no cuenta para la serie.
                }
            }
        }
        return prefijo + String.format("%04d", mayor + 1);
    }

    /**
     * Solo se puede eliminar la última factura de su año, para no dejar huecos
     * en la numeración (sirve para corregir un error antes de enviarla).
     */
    public static boolean sePuedeEliminar(List<Factura> emitidas, Factura f) {
        String siguiente = siguienteNumero(emitidas, f.fecha());
        int ultima = Integer.parseInt(siguiente.substring(siguiente.indexOf('-') + 1)) - 1;
        return f.numero().equals(f.fecha().getYear() + "-" + String.format("%04d", ultima));
    }
}
