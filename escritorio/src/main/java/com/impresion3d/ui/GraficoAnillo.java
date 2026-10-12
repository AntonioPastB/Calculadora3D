package com.impresion3d.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;

/**
 * Gráfico de anillo que muestra el peso de cada concepto sobre el coste total.
 */
class GraficoAnillo extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final float GROSOR = 22f;
    private static final double HUECO_GRADOS = 2.0;

    private final Color[] colores;
    private double[] valores = new double[0];
    private String textoCentral = "";
    private String subtituloCentral = "";

    GraficoAnillo(Color... colores) {
        this.colores = colores;
        setPreferredSize(new Dimension(190, 190));
    }

    void setDatos(double[] valores, String textoCentral, String subtituloCentral) {
        this.valores = valores.clone();
        this.textoCentral = textoCentral;
        this.subtituloCentral = subtituloCentral;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizado(g2);

        double tam = Math.min(getWidth(), getHeight()) - GROSOR - 4;
        double x = (getWidth() - tam) / 2;
        double y = (getHeight() - tam) / 2;

        g2.setStroke(new BasicStroke(GROSOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.setColor(Tema.BORDE);
        g2.draw(new Ellipse2D.Double(x, y, tam, tam));

        double total = 0;
        int noNulos = 0;
        for (double v : valores) {
            total += v;
            if (v > 0) {
                noNulos++;
            }
        }
        if (total > 0) {
            double inicio = 90;
            for (int i = 0; i < valores.length; i++) {
                double extension = -valores[i] / total * 360;
                if (extension == 0) {
                    continue;
                }
                double dibujada = noNulos > 1 && -extension > HUECO_GRADOS * 2
                        ? extension + HUECO_GRADOS : extension;
                g2.setColor(colores[i % colores.length]);
                g2.draw(new Arc2D.Double(x, y, tam, tam, inicio, dibujada, Arc2D.OPEN));
                inicio += extension;
            }
        }

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.fuente(Font.BOLD, 20f));
        FontMetrics fm = g2.getFontMetrics();
        int cy = getHeight() / 2;
        g2.drawString(textoCentral, (getWidth() - fm.stringWidth(textoCentral)) / 2, cy + 4);

        g2.setColor(Tema.TEXTO_SUAVE);
        g2.setFont(Tema.fuente(Font.PLAIN, 12f));
        fm = g2.getFontMetrics();
        g2.drawString(subtituloCentral, (getWidth() - fm.stringWidth(subtituloCentral)) / 2, cy + 22);
        g2.dispose();
    }
}
