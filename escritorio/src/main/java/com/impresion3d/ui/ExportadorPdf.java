package com.impresion3d.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.impresion3d.modelo.DocumentoPedido;
import com.impresion3d.modelo.ResumenPedido;
import com.impresion3d.modelo.Textos;
import com.impresion3d.persistencia.PdfImagenes;

/**
 * Dibuja el presupuesto de un pedido en hojas A4 y lo guarda como PDF.
 * Todas las coordenadas están en puntos (1/72 de pulgada).
 */
final class ExportadorPdf {

    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Textos.ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final double ANCHO = PdfImagenes.ANCHO_A4;
    private static final double ALTO = PdfImagenes.ALTO_A4;
    /** Píxeles por punto: 2,5 equivale a 180 ppp, suficiente para imprimir con nitidez. */
    private static final double ESCALA = 2.5;
    private static final double MARGEN = 48;
    private static final double DERECHA = ANCHO - MARGEN;
    private static final double LIMITE_INFERIOR = ALTO - 70;

    /** Bordes derechos de las columnas numéricas. */
    private static final double X_CANTIDAD = 365;
    private static final double X_PRECIO = 455;
    private static final double X_IMPORTE = DERECHA - 10;

    /** Lado de la miniatura de cada pieza, en puntos. */
    private static final double MINIATURA = 26;
    /** Verde de las facturas, para distinguirlas a simple vista de los presupuestos (morados). */
    static final Color FACTURA_1 = new Color(0x059669);
    static final Color FACTURA_2 = new Color(0x0D9488);

    private final DocumentoPedido doc;
    private final Function<String, BufferedImage> cargarImagen;
    private final List<BufferedImage> paginas = new ArrayList<>();
    private Graphics2D g;
    private double y;

    private ExportadorPdf(DocumentoPedido doc, Function<String, BufferedImage> cargarImagen) {
        this.doc = doc;
        this.cargarImagen = cargarImagen;
    }

    /**
     * @param cargarImagen devuelve la imagen de una pieza a partir de su nombre
     *                     (o {@code null}), para ponerla junto a su línea
     */
    static void exportar(DocumentoPedido doc, Path destino, Function<String, BufferedImage> cargarImagen)
            throws IOException {
        ExportadorPdf e = new ExportadorPdf(doc, cargarImagen);
        e.dibujar();
        String cliente = doc.cliente().nombre();
        PdfImagenes.escribir(e.paginas, destino, doc.titulo() + (cliente.isEmpty() ? "" : " - " + cliente));
    }

    /** Colores del documento: morado en los presupuestos, verde en las facturas. */
    private Color colorPrincipal() {
        return doc.esFactura() ? FACTURA_1 : Tema.ACENTO;
    }

    private Color colorSecundario() {
        return doc.esFactura() ? FACTURA_2 : Tema.ACENTO_2;
    }

    private void dibujar() {
        nuevaPagina();
        datosPartes();
        cabeceraTabla();
        for (DocumentoPedido.Linea l : doc.lineas()) {
            fila(l);
        }
        totales();
        g.dispose();
    }

    // ---------------------------------------------------------------- Partes del documento

    private void nuevaPagina() {
        if (g != null) {
            g.dispose();
        }
        BufferedImage img = new BufferedImage((int) Math.round(ANCHO * ESCALA),
                (int) Math.round(ALTO * ESCALA), BufferedImage.TYPE_INT_RGB);
        paginas.add(img);
        g = img.createGraphics();
        Tema.suavizado(g);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.scale(ESCALA, ESCALA);

        // Cabecera: logo, título y fecha.
        y = MARGEN;
        Graphics2D logo = (Graphics2D) g.create();
        logo.translate(MARGEN, y);
        logo.scale(0.5, 0.5);
        new Tema.IconoLogo(84).paintIcon(null, logo, 0, 0);
        logo.dispose();
        texto(doc.tipo().toString(), MARGEN + 56, y + 20, Font.BOLD, 22, colorPrincipal());
        if (doc.esFactura()) {
            texto("Nº " + doc.numero(), MARGEN + 56, y + 37, Font.BOLD, 11, Tema.TEXTO);
        } else {
            texto("Impresión 3D", MARGEN + 56, y + 36, Font.PLAIN, 10, Tema.TEXTO_SUAVE);
        }
        textoDerecha(doc.esFactura() ? "Fecha de expedición" : "Fecha", DERECHA, y + 12, Font.PLAIN, 9,
                Tema.TEXTO_SUAVE);
        textoDerecha(doc.fecha().format(FECHA), DERECHA, y + 27, Font.BOLD, 12, Tema.TEXTO);
        if (!doc.esFactura()) {
            textoDerecha("Válido hasta " + doc.validoHasta().format(FECHA), DERECHA, y + 41, Font.PLAIN, 9,
                    Tema.TEXTO_SUAVE);
        }
        if (paginas.size() > 1) {
            textoDerecha("Página " + paginas.size(), DERECHA, y + 54, Font.PLAIN, 8, Tema.TEXTO_SUAVE);
        }
        y += 60;
        g.setColor(colorPrincipal());
        g.fill(new Rectangle2D.Double(MARGEN, y, DERECHA - MARGEN, 2));
        y += 22;

        String pie = doc.esFactura()
                ? "Factura nº " + doc.numero() + " · " + doc.fecha().format(FECHA)
                : "Presupuesto válido hasta el " + doc.validoHasta().format(FECHA) + ". Este documento no es una factura.";
        texto(pie, MARGEN, ALTO - 30, Font.PLAIN, 7.5f, Tema.TEXTO_SUAVE);
        textoDerecha("Generado con Calculadora de impresión 3D", DERECHA, ALTO - 30, Font.PLAIN, 7.5f,
                Tema.TEXTO_SUAVE);
    }

    /** Datos del emisor (izquierda) y del cliente (derecha), si los hay. */
    private void datosPartes() {
        List<String> emisor = lineasEmisor();
        List<String> cliente = lineasCliente();
        if (emisor.isEmpty() && cliente.isEmpty()) {
            return;
        }
        double xCliente = ANCHO / 2 + 10;
        bloque(doc.esFactura() ? "EMISOR" : "DE", emisor, MARGEN);
        bloque("CLIENTE", cliente, xCliente);
        y += 22 + Math.max(emisor.size(), cliente.size()) * 13 + 10;
    }

    private void bloque(String titulo, List<String> lineas, double x) {
        if (lineas.isEmpty()) {
            return;
        }
        texto(titulo, x, y + 8, Font.BOLD, 8, Tema.TEXTO_SUAVE);
        double base = y + 23;
        for (int i = 0; i < lineas.size(); i++) {
            texto(recortar(lineas.get(i), i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 11.5f : 9.5f, ANCHO / 2 - 70),
                    x, base, i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 11.5f : 9.5f,
                    i == 0 ? Tema.TEXTO : Tema.TEXTO_SUAVE);
            base += 13;
        }
    }

    private List<String> lineasEmisor() {
        com.impresion3d.modelo.Emisor e = doc.emisor();
        return lineasDe(e.nombre(), e.nif(), e.direccion(), e.email(), e.telefono());
    }

    private List<String> lineasCliente() {
        com.impresion3d.modelo.Cliente c = doc.cliente();
        return lineasDe(c.nombre(), c.nif(), c.direccion(), c.email(), "");
    }

    /** Nombre, "NIF: …", cada línea de la dirección, email y teléfono (los que no estén vacíos). */
    private static List<String> lineasDe(String nombre, String nif, String direccion, String email, String telefono) {
        List<String> l = new ArrayList<>();
        if (!nombre.isEmpty()) {
            l.add(nombre);
        }
        if (!nif.isEmpty()) {
            l.add("NIF: " + nif);
        }
        for (String d : direccion.split("\\s*[\\r\\n]+\\s*")) {
            if (!d.isEmpty()) {
                l.add(d);
            }
        }
        if (!email.isEmpty()) {
            l.add(email);
        }
        if (!telefono.isEmpty()) {
            l.add(telefono);
        }
        return l;
    }

    private void cabeceraTabla() {
        g.setColor(Tema.FONDO);
        g.fill(new RoundRectangle2D.Double(MARGEN, y, DERECHA - MARGEN, 24, 8, 8));
        double base = y + 15.5;
        texto("Concepto", MARGEN + 10, base, Font.BOLD, 9, Tema.TEXTO_SUAVE);
        textoDerecha("Cant.", X_CANTIDAD, base, Font.BOLD, 9, Tema.TEXTO_SUAVE);
        textoDerecha("Precio ud.", X_PRECIO, base, Font.BOLD, 9, Tema.TEXTO_SUAVE);
        textoDerecha("Importe", X_IMPORTE, base, Font.BOLD, 9, Tema.TEXTO_SUAVE);
        y += 32;
    }

    private void fila(DocumentoPedido.Linea l) {
        boolean conDetalle = !l.detalle().isEmpty();
        double alto = conDetalle ? 34 : 22;
        if (y + alto > LIMITE_INFERIOR) {
            nuevaPagina();
            cabeceraTabla();
        }
        // Miniatura de la pieza, si tiene imagen; el texto se desplaza a su derecha.
        double xTexto = MARGEN + 10;
        BufferedImage imagen = l.imagen() == null ? null : cargarImagen.apply(l.imagen());
        if (imagen != null) {
            int px = (int) Math.round(MINIATURA * ESCALA);
            Graphics2D gi = (Graphics2D) g.create();
            gi.translate(xTexto, y + 2);
            gi.scale(1 / ESCALA, 1 / ESCALA);
            gi.drawImage(Tema.miniatura(imagen, px), 0, 0, null);
            gi.dispose();
            xTexto += MINIATURA + 8;
        }
        double anchoConcepto = X_CANTIDAD - 50 - xTexto;
        boolean descuento = l.importe() < 0;
        texto(recortar(l.concepto(), Font.BOLD, 10.5f, anchoConcepto), xTexto, y + 12, Font.BOLD, 10.5f,
                descuento ? Tema.C_AMORTIZACION.darker() : Tema.TEXTO);
        if (conDetalle) {
            texto(recortar(l.detalle(), Font.PLAIN, 8.5f, X_IMPORTE - xTexto), xTexto, y + 26,
                    Font.PLAIN, 8.5f, Tema.TEXTO_SUAVE);
        }
        if (l.cantidad() > 0) {
            textoDerecha(Integer.toString(l.cantidad()), X_CANTIDAD, y + 12, Font.PLAIN, 10, Tema.TEXTO);
        }
        if (l.precioUnitario() != null) {
            textoDerecha(EUROS.format(l.precioUnitario()), X_PRECIO, y + 12, Font.PLAIN, 10, Tema.TEXTO);
        }
        textoDerecha((descuento ? "−" : "") + EUROS.format(Math.abs(l.importe())), X_IMPORTE, y + 12,
                Font.BOLD, 10, descuento ? Tema.C_AMORTIZACION.darker() : Tema.TEXTO);

        y += alto;
        g.setColor(Tema.BORDE);
        g.setStroke(new java.awt.BasicStroke(0.7f));
        g.draw(new Line2D.Double(MARGEN, y, DERECHA, y));
        y += 8;
    }

    private void totales() {
        ResumenPedido r = doc.resumen();
        if (y + 150 > LIMITE_INFERIOR) {
            nuevaPagina();
        }
        y += 8;
        double x0 = 320;
        double xEtiqueta = x0 + 12;

        y += 14;
        texto("Base imponible", xEtiqueta, y, Font.PLAIN, 10, Tema.TEXTO_SUAVE);
        textoDerecha(EUROS.format(r.baseImponible()), X_IMPORTE, y, Font.PLAIN, 10, Tema.TEXTO);
        y += 18;
        texto("IVA (" + DocumentoPedido.formatoPorcentaje(r.porcentajeIva()) + ")", xEtiqueta, y, Font.PLAIN,
                10, Tema.TEXTO_SUAVE);
        textoDerecha(EUROS.format(r.iva()), X_IMPORTE, y, Font.PLAIN, 10, Tema.TEXTO);
        y += 14;

        // Caja del total con el degradado de la aplicación.
        g.setPaint(new java.awt.GradientPaint((float) x0, (float) y, colorPrincipal(), (float) DERECHA,
                (float) (y + 40), colorSecundario()));
        g.fill(new RoundRectangle2D.Double(x0, y, DERECHA - x0, 40, 12, 12));
        texto("TOTAL", xEtiqueta, y + 25, Font.BOLD, 11, new Color(255, 255, 255, 220));
        textoDerecha(EUROS.format(r.total()), X_IMPORTE, y + 27, Font.BOLD, 18, Color.WHITE);
        y += 56;

        int piezas = r.lineas().size();
        textoDerecha("IVA incluido · " + piezas + (piezas == 1 ? " pieza · " : " piezas · ") + r.unidades()
                + (r.unidades() == 1 ? " unidad" : " unidades"), X_IMPORTE, y, Font.PLAIN, 9, Tema.TEXTO_SUAVE);
        y += 20;
    }

    // ---------------------------------------------------------------- Utilidades de texto

    private void texto(String s, double x, double base, int estilo, float tam, Color color) {
        g.setFont(Tema.fuente(estilo, tam));
        g.setColor(color);
        g.drawString(s, (float) x, (float) base);
    }

    private void textoDerecha(String s, double xDerecha, double base, int estilo, float tam, Color color) {
        g.setFont(Tema.fuente(estilo, tam));
        FontMetrics fm = g.getFontMetrics();
        texto(s, xDerecha - fm.getStringBounds(s, g).getWidth(), base, estilo, tam, color);
    }

    /** Acorta el texto con "…" si no cabe en el ancho indicado. */
    private String recortar(String s, int estilo, float tam, double ancho) {
        g.setFont(Tema.fuente(estilo, tam));
        FontMetrics fm = g.getFontMetrics();
        if (fm.getStringBounds(s, g).getWidth() <= ancho) {
            return s;
        }
        String r = s;
        while (r.length() > 1 && fm.getStringBounds(r + "…", g).getWidth() > ancho) {
            r = r.substring(0, r.length() - 1);
        }
        return r.strip() + "…";
    }
}
