package com.impresion3d.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lógica de cálculo del precio de una impresión 3D, independiente de la interfaz.
 */
public final class CalculadoraPrecio {

    private CalculadoraPrecio() {
    }

    /** Precio de una unidad sin gastos adicionales. */
    public static DesglosePrecio calcular(DatosImpresion d) {
        return calcular(d, Collections.emptyList());
    }

    /** Precio de una unidad con sus gastos adicionales (que se suman sin margen). */
    public static DesglosePrecio calcular(DatosImpresion d, List<GastoAdicional> gastosPieza) {
        double material = d.pesoGramos() / 1000.0 * d.precioFilamentoKg();
        double electricidad = d.potenciaW() / 1000.0 * d.horasImpresion() * d.precioKwh();
        double amortizacion = (d.precioImpresora() / d.vidaUtilHoras() + d.mantenimientoHora())
                * d.horasImpresion();
        double manoObra = d.horasManoObra() * d.precioHoraManoObra();

        // El riesgo de fallos se trata como un gasto estable: un porcentaje fijo
        // sobre todo el coste de producción, incluida la mano de obra.
        double costeProduccion = material + electricidad + amortizacion + manoObra;
        double fallos = costeProduccion * d.porcentajeFallos() / 100.0;

        double costeTotal = costeProduccion + fallos;
        double beneficio = costeTotal * d.margenBeneficio() / 100.0;
        double extras = gastosPieza.stream().mapToDouble(GastoAdicional::importe).sum();
        double precioSinIva = costeTotal + beneficio + extras;
        double iva = precioSinIva * d.iva() / 100.0;

        return new DesglosePrecio(material, electricidad, amortizacion, manoObra, fallos,
                costeTotal, beneficio, extras, precioSinIva, iva, precioSinIva + iva);
    }

    /** Precio de una unidad de una pieza con los datos comunes del taller. */
    public static DesglosePrecio calcular(Pieza pieza, Taller taller) {
        return calcular(pieza.datos(taller), pieza.gastos());
    }

    /**
     * Calcula el presupuesto completo: cada pieza por sus unidades, con el
     * descuento por volumen que le corresponda según sus unidades; después,
     * los gastos del pedido y el IVA de todo.
     */
    public static ResumenPedido calcularPedido(Taller taller, Pedido pedido) {
        List<ResumenPedido.LineaPieza> lineas = new ArrayList<>();
        double subtotal = 0;
        double descuentos = 0;
        for (Pieza pieza : pedido.piezas()) {
            DesglosePrecio desglose = calcular(pieza, taller);
            double unitario = desglose.precioSinIva();
            double importe = unitario * pieza.cantidad();
            TramoDescuento tramo = pedido.tramoPara(pieza.cantidad()).orElse(null);
            double descuento = tramo == null ? 0 : importe * tramo.porcentaje() / 100.0;
            lineas.add(new ResumenPedido.LineaPieza(pieza, desglose, unitario, importe, tramo, descuento));
            subtotal += importe;
            descuentos += descuento;
        }

        int unidades = pedido.unidades();
        double gastos = pedido.gastos().stream().mapToDouble(g -> g.totalPara(unidades)).sum();
        double base = subtotal - descuentos + gastos;
        double iva = base * taller.iva() / 100.0;
        return new ResumenPedido(lineas, subtotal, descuentos, gastos, base, taller.iva(), iva, base + iva, unidades);
    }
}
