package com.impresion3d.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.impresion3d.modelo.Cliente;
import com.impresion3d.modelo.DocumentoPedido;
import com.impresion3d.modelo.Emisor;
import com.impresion3d.modelo.ResumenPedido;
import com.impresion3d.modelo.Textos;

/**
 * Exporta un pedido a CSV para abrirlo con Excel: separado por ";", con coma
 * decimal y codificación UTF-8 con BOM (para que Excel lea bien los acentos).
 */
public final class ExportadorCsv {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Marca de orden de bytes: indica a Excel que el archivo está en UTF-8. */
    private static final String BOM = "﻿";

    private ExportadorCsv() {
    }

    public static void exportar(DocumentoPedido doc, Path destino) throws IOException {
        Files.write(destino, generar(doc).getBytes(StandardCharsets.UTF_8));
    }

    /** Contenido completo del CSV. */
    public static String generar(DocumentoPedido doc) {
        DecimalFormat num = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Textos.ES));
        ResumenPedido r = doc.resumen();
        List<String> f = new ArrayList<>();

        if (doc.esFactura()) {
            f.add(fila("Factura", doc.numero()));
            f.add(fila("Fecha", doc.fecha().format(FECHA)));
        } else {
            f.add(fila("Presupuesto", doc.fecha().format(FECHA)));
            f.add(fila("Válido hasta", doc.validoHasta().format(FECHA)));
        }
        Emisor e = doc.emisor();
        if (!e.vacio()) {
            f.add(fila("Emisor", e.nombre(), e.nif(), e.direccion().replaceAll("\\s*[\\r\\n]+\\s*", ", ")));
        }
        Cliente c = doc.cliente();
        if (!c.vacio()) {
            f.add(fila("Cliente", c.nombre(), c.nif(), c.direccion().replaceAll("\\s*[\\r\\n]+\\s*", ", ")));
        }
        f.add("");
        f.add(fila("Concepto", "Detalle", "Cantidad", "Precio unitario (€)", "Importe (€)"));
        for (DocumentoPedido.Linea l : doc.lineas()) {
            f.add(fila(l.concepto(), l.detalle(),
                    l.cantidad() > 0 ? Integer.toString(l.cantidad()) : "",
                    l.precioUnitario() != null ? num.format(l.precioUnitario()) : "",
                    num.format(l.importe())));
        }
        f.add("");
        f.add(fila("", "", "", "Base imponible", num.format(r.baseImponible())));
        f.add(fila("", "", "", "IVA (" + DocumentoPedido.formatoPorcentaje(r.porcentajeIva()) + ")",
                num.format(r.iva())));
        f.add(fila("", "", "", "Total", num.format(r.total())));
        f.add(fila("", "", "", "Unidades", Integer.toString(r.unidades())));

        return BOM + String.join("\r\n", f) + "\r\n";
    }

    private static String fila(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escapar(campos[i]));
        }
        return sb.toString();
    }

    /** Entrecomilla los campos que contienen el separador, comillas o saltos de línea. */
    private static String escapar(String campo) {
        if (campo.contains(";") || campo.contains("\"") || campo.contains("\n") || campo.contains("\r")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }
}
