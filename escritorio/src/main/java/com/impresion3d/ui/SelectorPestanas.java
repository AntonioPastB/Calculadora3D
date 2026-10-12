package com.impresion3d.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

/**
 * Control segmentado (tipo pestañas) que muestra una de varias páginas.
 * {@link #getBarra()} es la fila de botones y {@link #getPaginas()} el
 * contenedor de las páginas.
 */
class SelectorPestanas {

    private static final Color FONDO_BARRA = new Color(0xE2E8F0);

    private final JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizado(g2);
            g2.setColor(FONDO_BARRA);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
            g2.dispose();
        }
    };
    private final CardLayout cartas = new CardLayout();
    private final JPanel paginas = new JPanel(cartas);
    private final ButtonGroup grupo = new ButtonGroup();
    private final List<JToggleButton> botones = new ArrayList<>();

    SelectorPestanas() {
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(4, 4, 4, 4));
        paginas.setOpaque(false);
    }

    JComponent getBarra() {
        // Envoltorio para que la barra no ocupe todo el ancho.
        JPanel envoltorio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envoltorio.setOpaque(false);
        envoltorio.add(barra);
        return envoltorio;
    }

    JComponent getPaginas() {
        return paginas;
    }

    void anadir(String titulo, JComponent pagina) {
        String id = "p" + botones.size();
        JToggleButton b = new JToggleButton(titulo) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Tema.suavizado(g2);
                    g2.setColor(new Color(15, 23, 42, 18));
                    g2.fill(new RoundRectangle2D.Double(0, 1.5, getWidth(), getHeight() - 1, 12, 12));
                    g2.setColor(Color.WHITE);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() - 1, 12, 12));
                    g2.dispose();
                }
                setForeground(isSelected() ? Tema.TEXTO
                        : getModel().isRollover() ? Tema.ACENTO : Tema.TEXTO_SUAVE);
                super.paintComponent(g);
            }
        };
        b.setFont(Tema.fuente(Font.BOLD, 13f));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setBorder(new EmptyBorder(7, 18, 7, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> cartas.show(paginas, id));

        grupo.add(b);
        botones.add(b);
        barra.add(b);
        paginas.add(pagina, id);
        if (botones.size() == 1) {
            b.setSelected(true);
        }
    }

    /** Muestra la pestaña indicada, como si se hubiera pulsado. */
    void mostrar(int indice) {
        botones.get(indice).setSelected(true);
        cartas.show(paginas, "p" + indice);
    }

    /** Cambia el texto de una pestaña (por ejemplo, para mostrar la cantidad). */
    void setTitulo(int indice, String titulo) {
        botones.get(indice).setText(titulo);
    }
}
