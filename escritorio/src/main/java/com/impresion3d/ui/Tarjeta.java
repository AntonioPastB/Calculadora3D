package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Panel con esquinas redondeadas y sombra suave. Puede tener fondo blanco
 * con título o un degradado de color.
 */
class Tarjeta extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int RADIO = 20;
    private static final int SOMBRA = 6;

    private final Color degradado1;
    private final Color degradado2;

    /** Tarjeta blanca con un título y un icono de color. */
    Tarjeta(String titulo, Color acento) {
        this((Color) null, null);
        JLabel l = Tema.etiqueta(titulo, Font.BOLD, 15f, Tema.TEXTO);
        l.setIcon(new Tema.IconoPunto(acento, 24));
        l.setIconTextGap(10);
        l.setBorder(new EmptyBorder(0, 0, 12, 0));
        add(l, BorderLayout.NORTH);
    }

    /** Tarjeta con fondo degradado (o blanca si los colores son nulos) y sin título. */
    Tarjeta(Color degradado1, Color degradado2) {
        super(new BorderLayout());
        this.degradado1 = degradado1;
        this.degradado2 = degradado2;
        setOpaque(false);
        setBorder(new EmptyBorder(SOMBRA + 14, SOMBRA + 18, SOMBRA + 16, SOMBRA + 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizado(g2);
        int w = getWidth();
        int h = getHeight();

        // Sombra: capas semitransparentes superpuestas, más densas hacia el centro.
        for (int i = SOMBRA; i > 0; i--) {
            int m = SOMBRA - i;
            g2.setColor(new Color(15, 23, 42, 5));
            g2.fill(new RoundRectangle2D.Double(m, m + 3, w - 2 * m, h - 2 * m - 3, RADIO + i * 2, RADIO + i * 2));
        }

        RoundRectangle2D cuerpo = new RoundRectangle2D.Double(SOMBRA, SOMBRA - 2, w - 2 * SOMBRA,
                h - 2 * SOMBRA, RADIO, RADIO);
        if (degradado1 != null) {
            g2.setPaint(new GradientPaint(0, 0, degradado1, w, h, degradado2));
            g2.fill(cuerpo);
        } else {
            g2.setColor(Tema.TARJETA);
            g2.fill(cuerpo);
            g2.setColor(Tema.BORDE);
            g2.draw(cuerpo);
        }
        g2.dispose();
    }
}
