package com.impresion3d.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/**
 * Muestra una imagen ajustada al espacio disponible, con esquinas redondeadas.
 * Sin imagen, dibuja un recuadro discontinuo con un texto de ayuda.
 */
class PanelImagen extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final Color BORDE_DISCONTINUO = new Color(0xCBD5E1);

    private BufferedImage imagen;
    private BufferedImage escalada;
    private String textoVacio;

    PanelImagen(String textoVacio, int ancho, int alto) {
        this.textoVacio = textoVacio;
        setPreferredSize(new Dimension(ancho, alto));
    }

    void setImagen(BufferedImage imagen) {
        this.imagen = imagen;
        this.escalada = null;
        repaint();
    }

    BufferedImage getImagen() {
        return imagen;
    }

    void setTextoVacio(String texto) {
        this.textoVacio = texto;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizado(g2);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 1.5, 18, 18);

        g2.setColor(Tema.FONDO);
        g2.fill(forma);

        if (imagen != null) {
            double escala = Math.min((double) w / imagen.getWidth(), (double) h / imagen.getHeight());
            int dw = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
            int dh = Math.max(1, (int) Math.round(imagen.getHeight() * escala));
            if (escalada == null || escalada.getWidth() != dw || escalada.getHeight() != dh) {
                escalada = Tema.escalar(imagen, dw, dh);
            }
            Shape clipAnterior = g2.getClip();
            g2.clip(forma);
            g2.drawImage(escalada, (w - dw) / 2, (h - dh) / 2, null);
            g2.setClip(clipAnterior);
        } else {
            g2.setColor(BORDE_DISCONTINUO);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] { 6f, 5f }, 0f));
            g2.draw(forma);
            dibujarTexto(g2, w, h);
        }
        g2.dispose();
    }

    /** Pequeño dibujo de una foto y el texto de ayuda, centrados. */
    private void dibujarTexto(Graphics2D g2, int w, int h) {
        String[] lineas = textoVacio == null ? new String[0] : textoVacio.split("\n");
        g2.setFont(Tema.fuente(Font.PLAIN, 13f));
        FontMetrics fm = g2.getFontMetrics();
        int altoTexto = lineas.length * fm.getHeight();
        int altoIcono = h >= 120 ? 34 : 0;
        int y = (h - altoIcono - altoTexto - (altoIcono > 0 ? 10 : 0)) / 2;

        if (altoIcono > 0) {
            double ix = w / 2.0 - 22;
            g2.setColor(BORDE_DISCONTINUO);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Double(ix, y, 44, altoIcono, 8, 8));
            Path2D montana = new Path2D.Double();
            montana.moveTo(ix + 6, y + altoIcono - 6);
            montana.lineTo(ix + 18, y + 14);
            montana.lineTo(ix + 26, y + 22);
            montana.lineTo(ix + 31, y + 17);
            montana.lineTo(ix + 38, y + altoIcono - 6);
            g2.draw(montana);
            g2.fill(new Ellipse2D.Double(ix + 30, y + 6, 6, 6));
            y += altoIcono + 10;
        }

        g2.setColor(Tema.TEXTO_SUAVE);
        for (String linea : lineas) {
            g2.drawString(linea, (w - fm.stringWidth(linea)) / 2, y + fm.getAscent());
            y += fm.getHeight();
        }
    }
}
