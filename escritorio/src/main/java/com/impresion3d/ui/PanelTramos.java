package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;

import com.impresion3d.modelo.TramoDescuento;

/**
 * Lista editable de descuentos por volumen ("desde N uds, X % de descuento").
 * Resalta el tramo que se está aplicando al pedido actual.
 */
class PanelTramos extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color FONDO_ACTIVO = new Color(0xEEF2FF);
    private static final int ANCHO_COLUMNA = 100;
    private static final int MARGEN_FILA = 8;

    /** Marca de verificación para el tramo aplicado. */
    private static final javax.swing.Icon ICONO_APLICADO = new javax.swing.Icon() {
        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            Tema.suavizado(g2);
            g2.setColor(Tema.ACENTO);
            g2.fill(new java.awt.geom.Ellipse2D.Double(x, y, 18, 18));
            g2.setColor(Color.WHITE);
            g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D v = new java.awt.geom.Path2D.Double();
            v.moveTo(x + 5, y + 9.5);
            v.lineTo(x + 8, y + 12.5);
            v.lineTo(x + 13, y + 6);
            g2.draw(v);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    };

    private final JPanel filas = new JPanel(new GridBagLayout());
    private final List<Fila> lista = new ArrayList<>();
    private final JLabel lblVacio = Tema.etiqueta(
            "Sin descuentos por volumen.", Font.ITALIC, 13f, Tema.TEXTO_SUAVE);
    private final transient Runnable alCambiar;
    private List<TramoDescuento> activos = new ArrayList<>();

    /** @param alCambiar se llama cada vez que cambia algún tramo */
    PanelTramos(Runnable alCambiar) {
        super(new BorderLayout(0, 10));
        this.alCambiar = alCambiar;
        setOpaque(false);
        filas.setOpaque(false);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(filas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(contenedor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        // Altura para unos tres tramos; si hay más, aparece la barra de desplazamiento.
        scroll.setPreferredSize(new Dimension(100, 160));

        JButton btnAnadir = Tema.boton("+ Añadir descuento", false);
        btnAnadir.addActionListener(e -> {
            int siguiente = lista.stream().mapToInt(f -> f.cantidad()).max().orElse(0) + 10;
            double pct = lista.stream().mapToDouble(f -> f.porcentaje()).max().orElse(0) + 5;
            anadirFila(new TramoDescuento(siguiente, Math.min(100, pct)));
            alCambiar.run();
        });
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(btnAnadir, BorderLayout.WEST);

        add(scroll, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        reconstruir();
    }

    /** Tramos ordenados de menor a mayor cantidad. */
    List<TramoDescuento> getTramos() {
        List<TramoDescuento> tramos = new ArrayList<>();
        for (Fila f : lista) {
            tramos.add(new TramoDescuento(f.cantidad(), f.porcentaje()));
        }
        tramos.sort(Comparator.comparingInt(TramoDescuento::cantidadMinima));
        return tramos;
    }

    void setTramos(List<TramoDescuento> tramos) {
        lista.clear();
        tramos.stream().sorted(Comparator.comparingInt(TramoDescuento::cantidadMinima))
                .forEach(this::anadirFila);
        reconstruir();
    }

    /** Marca qué tramos se están aplicando a alguna pieza. */
    void setTramosActivos(java.util.Collection<TramoDescuento> tramos) {
        this.activos = new java.util.ArrayList<>(tramos);
        for (Fila f : lista) {
            boolean esActivo = activos.stream().anyMatch(
                    t -> f.cantidad() == t.cantidadMinima() && f.porcentaje() == t.porcentaje());
            f.fondo.setOpaque(esActivo);
            f.fondo.setBackground(FONDO_ACTIVO);
            f.marca.setIcon(esActivo ? ICONO_APLICADO : null);
            f.marca.setToolTipText(esActivo ? "Este descuento se aplica a alguna pieza" : null);
            f.fondo.repaint();
        }
    }

    private void anadirFila(TramoDescuento t) {
        lista.add(new Fila(t));
        reconstruir();
    }

    private void quitarFila(Fila f) {
        lista.remove(f);
        reconstruir();
        alCambiar.run();
    }

    private void reconstruir() {
        filas.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (lista.isEmpty()) {
            c.insets = new Insets(6, 2, 6, 0);
            filas.add(lblVacio, c);
        } else {
            c.insets = new Insets(0, 0, 2, 0);
            filas.add(cabecera(), c);
            for (Fila f : lista) {
                filas.add(f.fondo, c);
            }
        }
        setTramosActivos(activos);
        filas.revalidate();
        filas.repaint();
    }

    /** Cabecera de las columnas, alineada con las filas. */
    private static JPanel cabecera() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, MARGEN_FILA, 2, 4));
        String[] textos = { "Desde (uds)", "Descuento (%)" };
        for (int i = 0; i < textos.length; i++) {
            JLabel l = Tema.etiqueta(textos[i], Font.BOLD, 12f, Tema.TEXTO_SUAVE);
            l.setPreferredSize(new Dimension(ANCHO_COLUMNA, l.getPreferredSize().height));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.insets = new Insets(0, 0, 0, 8);
            c.anchor = GridBagConstraints.WEST;
            p.add(l, c);
        }
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridx = textos.length;
        relleno.weightx = 1;
        p.add(new JLabel(), relleno);
        return p;
    }

    /** Componentes de una fila: [desde N uds] [X %] (✓ si se aplica) ×. */
    private final class Fila {
        final JSpinner spCantidad = Tema.campoNumerico(10, 1, 1000000, 1);
        final JSpinner spPorcentaje = Tema.campoNumerico(5, 0, 100, 1);
        final JButton quitar = Tema.botonQuitar("Quitar este descuento");
        final JLabel marca = new JLabel();
        final JPanel fondo = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Fondo redondeado para el tramo aplicado.
                if (isOpaque()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    Tema.suavizado(g2);
                    g2.setColor(getBackground());
                    g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                    g2.dispose();
                }
            }
        };

        Fila(TramoDescuento t) {
            spCantidad.setValue((double) t.cantidadMinima());
            spPorcentaje.setValue(t.porcentaje());
            spCantidad.addChangeListener(e -> alCambiar.run());
            spPorcentaje.addChangeListener(e -> alCambiar.run());
            quitar.addActionListener(e -> quitarFila(this));

            // Mismo ancho fijo que las cabeceras, para que las columnas cuadren.
            spCantidad.setPreferredSize(new Dimension(ANCHO_COLUMNA, spCantidad.getPreferredSize().height));
            spPorcentaje.setPreferredSize(new Dimension(ANCHO_COLUMNA, spPorcentaje.getPreferredSize().height));

            fondo.setOpaque(false);
            fondo.setBorder(BorderFactory.createEmptyBorder(4, MARGEN_FILA, 4, 4));
            fondo.add(spCantidad, celda(0, 0));
            fondo.add(spPorcentaje, celda(1, 0));
            fondo.add(marca, celda(2, 1));
            fondo.add(quitar, celda(3, 0));
        }

        int cantidad() {
            return (int) Math.round(((Number) spCantidad.getValue()).doubleValue());
        }

        double porcentaje() {
            return ((Number) spPorcentaje.getValue()).doubleValue();
        }

        private GridBagConstraints celda(int col, double peso) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = col;
            c.insets = new Insets(0, 0, 0, 8);
            c.anchor = GridBagConstraints.WEST;
            c.weightx = peso == 1 ? 1 : 0;
            return c;
        }
    }
}
