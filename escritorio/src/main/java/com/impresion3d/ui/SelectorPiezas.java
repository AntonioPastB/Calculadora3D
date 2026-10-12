package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import com.impresion3d.modelo.Pieza;

/**
 * Fila de "fichas", una por pieza del presupuesto, para elegir cuál se edita;
 * más los botones para añadir, duplicar y eliminar piezas.
 */
class SelectorPiezas extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel fichas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final transient IntConsumer alSeleccionar;
    private final JButton btnEliminar = Tema.boton("Eliminar", false);

    SelectorPiezas(IntConsumer alSeleccionar, Runnable alAnadir, Runnable alDuplicar, Runnable alEliminar) {
        super(new BorderLayout(10, 0));
        this.alSeleccionar = alSeleccionar;
        setOpaque(false);
        fichas.setOpaque(false);

        JScrollPane scroll = new JScrollPane(fichas, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        JButton btnAnadir = Tema.boton("+ Añadir pieza", true);
        JButton btnDuplicar = Tema.boton("Duplicar", false);
        btnAnadir.addActionListener(e -> alAnadir.run());
        btnDuplicar.addActionListener(e -> alDuplicar.run());
        btnEliminar.addActionListener(e -> alEliminar.run());
        btnDuplicar.setToolTipText("Crea una copia de la pieza seleccionada");
        btnEliminar.setToolTipText("Quita la pieza seleccionada del presupuesto");

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        botones.add(btnDuplicar);
        botones.add(btnEliminar);
        botones.add(btnAnadir);

        add(scroll, BorderLayout.CENTER);
        add(botones, BorderLayout.EAST);
    }

    /** Vuelve a dibujar las fichas con las piezas actuales. */
    void mostrar(List<Pieza> piezas, int seleccionada) {
        fichas.removeAll();
        for (int i = 0; i < piezas.size(); i++) {
            Pieza p = piezas.get(i);
            int indice = i;
            Ficha f = new Ficha(p.nombreVisible(i) + "  ×" + p.cantidad(), i == seleccionada);
            f.addActionListener(e -> alSeleccionar.accept(indice));
            fichas.add(f);
        }
        btnEliminar.setEnabled(piezas.size() > 1);
        fichas.revalidate();
        fichas.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // Altura fija de una fila, aunque las fichas no quepan a lo ancho (se desplazan).
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 44);
    }

    /** Ficha de una pieza: rellena de color si está seleccionada. */
    private static final class Ficha extends JButton {
        private static final long serialVersionUID = 1L;
        private final boolean seleccionada;

        Ficha(String texto, boolean seleccionada) {
            super(texto);
            this.seleccionada = seleccionada;
            setFont(Tema.fuente(Font.BOLD, 13f));
            setForeground(seleccionada ? Color.WHITE : Tema.TEXTO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizado(g2);
            RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5,
                    getHeight() - 2, getHeight() - 2);
            if (seleccionada) {
                g2.setPaint(new java.awt.GradientPaint(0, 0, Tema.ACENTO, getWidth(), getHeight(), Tema.ACENTO_2));
                g2.fill(forma);
            } else {
                g2.setColor(Tema.TARJETA);
                g2.fill(forma);
                g2.setColor(getModel().isRollover() ? Tema.ACENTO : Tema.BORDE);
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.draw(forma);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
