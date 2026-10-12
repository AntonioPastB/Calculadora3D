package com.impresion3d.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Presupuesto o factura listo para exportar: datos del emisor y del cliente y
 * las líneas que verá el cliente (sin el desglose interno de costes).
 *
 * @param numero número de factura, o cadena vacía en los presupuestos
 * @param emisor tus datos (pueden estar vacíos en un presupuesto)
 */
public record DocumentoPedido(
        Tipo tipo,
        String numero,
        LocalDate fecha,
        Emisor emisor,
        Cliente cliente,
        Pedido pedido,
        ResumenPedido resumen) {

    /** Días que es válido un presupuesto desde su fecha. */
    public static final int VALIDEZ_PRESUPUESTO_DIAS = 30;

    public enum Tipo {
        PRESUPUESTO("Presupuesto"), FACTURA("Factura");

        private final String nombre;

        Tipo(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    /**
     * Línea del documento.
     *
     * @param detalle        texto secundario (puede estar vacío)
     * @param cantidad       unidades, o 0 si la línea no tiene cantidad (descuentos)
     * @param precioUnitario precio por unidad sin IVA, o {@code null} si no aplica
     * @param importe        importe de la línea sin IVA (negativo en los descuentos)
     * @param imagen         imagen de la pieza en el almacén, o {@code null}
     */
    public record Linea(String concepto, String detalle, int cantidad, Double precioUnitario, double importe,
            String imagen) {
    }

    public DocumentoPedido {
        numero = Textos.limpiar(numero);
        if (emisor == null) {
            emisor = Emisor.VACIO;
        }
        if (cliente == null) {
            cliente = Cliente.conNombre("");
        }
    }

    public static DocumentoPedido presupuesto(LocalDate fecha, Emisor emisor, String cliente, Pedido pedido,
            ResumenPedido resumen) {
        return new DocumentoPedido(Tipo.PRESUPUESTO, "", fecha, emisor, Cliente.conNombre(cliente), pedido, resumen);
    }

    public static DocumentoPedido factura(Factura f) {
        return new DocumentoPedido(Tipo.FACTURA, f.numero(), f.fecha(), f.emisor(), f.cliente(), f.pedido(),
                f.resumen());
    }

    public boolean esFactura() {
        return tipo == Tipo.FACTURA;
    }

    /** Hasta cuándo es válido el presupuesto. */
    public LocalDate validoHasta() {
        return fecha.plusDays(VALIDEZ_PRESUPUESTO_DIAS);
    }

    /** Título para el documento y el nombre del archivo: "Presupuesto" o "Factura 2026-0001". */
    public String titulo() {
        return esFactura() ? "Factura " + numero : "Presupuesto";
    }

    public List<Linea> lineas() {
        List<Linea> lineas = new ArrayList<>();
        List<ResumenPedido.LineaPieza> piezas = resumen.lineas();
        for (int i = 0; i < piezas.size(); i++) {
            ResumenPedido.LineaPieza lp = piezas.get(i);
            Pieza p = lp.pieza();
            String concepto = p.nombre().isEmpty()
                    ? (piezas.size() == 1 ? "Pieza impresa en 3D" : "Pieza impresa en 3D (" + (i + 1) + ")")
                    : p.nombre();

            StringBuilder detalle = new StringBuilder(p.resumenTecnico()).append(" de impresión");
            if (!p.gastos().isEmpty()) {
                detalle.append(" · Incluye: ").append(p.gastos().stream()
                        .map(g -> g.concepto().isEmpty() ? "gasto adicional" : g.concepto())
                        .collect(Collectors.joining(", ")));
            }
            lineas.add(new Linea(concepto, detalle.toString(), p.cantidad(), lp.precioUnitario(), lp.subtotal(),
                    p.imagen()));

            if (lp.tramo() != null && lp.descuento() > 0) {
                lineas.add(new Linea("Descuento por volumen",
                        formatoPorcentaje(lp.porcentajeDescuento()) + " en " + concepto + " (a partir de "
                                + lp.tramo().cantidadMinima() + " unidades)",
                        0, null, -lp.descuento(), null));
            }
        }

        int unidades = pedido.unidades();
        for (GastoAdicional g : pedido.gastos()) {
            lineas.add(new Linea(g.concepto().isEmpty() ? "Gasto adicional" : g.concepto(),
                    g.porUnidad() ? "Por unidad" : "", g.porUnidad() ? unidades : 1, g.importe(),
                    g.totalPara(unidades), null));
        }
        return lineas;
    }

    public static String formatoPorcentaje(double v) {
        return (v == Math.rint(v) ? Long.toString((long) v) : String.format("%.1f", v)) + " %";
    }
}
