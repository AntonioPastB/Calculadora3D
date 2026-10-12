package com.impresion3d.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSpinnerUI;

/**
 * Colores, fuentes y pequeños componentes con el estilo visual de la aplicación.
 */
final class Tema {

    static final Color FONDO = new Color(0xF1F4F9);
    static final Color TARJETA = Color.WHITE;
    static final Color TEXTO = new Color(0x1E293B);
    static final Color TEXTO_SUAVE = new Color(0x64748B);
    static final Color BORDE = new Color(0xE2E8F0);
    static final Color ACENTO = new Color(0x4F46E5);
    static final Color ACENTO_2 = new Color(0x7C3AED);
    static final Color ERROR = new Color(0xDC2626);

    static final Color C_MATERIAL = new Color(0x6366F1);
    static final Color C_ELECTRICIDAD = new Color(0xF59E0B);
    static final Color C_AMORTIZACION = new Color(0x10B981);
    static final Color C_FALLOS = new Color(0xEF4444);
    static final Color C_MANO_OBRA = new Color(0x06B6D4);
    static final Color C_GASTOS_PIEZA = new Color(0xEC4899);

    private Tema() {
    }

    static Font fuente(int estilo, float tamano) {
        return new Font("Segoe UI", estilo, 12).deriveFont(tamano);
    }

    static void suavizado(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static JLabel etiqueta(String texto, int estilo, float tamano, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(estilo, tamano));
        l.setForeground(color);
        return l;
    }

    /** Campo numérico con borde redondeado, flechas planas y soporte para la rueda del ratón. */
    static JSpinner campoNumerico(double valor, double min, double max, double paso) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, min, max, paso));
        s.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                JButton b = flecha(true);
                installNextButtonListeners(b);
                return b;
            }

            @Override
            protected Component createPreviousButton() {
                JButton b = flecha(false);
                installPreviousButtonListeners(b);
                return b;
            }
        });

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(s, "#,##0.##");
        JFormattedTextField campo = editor.getTextField();
        campo.setColumns(6);
        campo.setBorder(new EmptyBorder(0, 0, 0, 0));
        campo.setFont(fuente(Font.PLAIN, 14f));
        campo.setForeground(TEXTO);
        campo.setBackground(TARJETA);
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        editor.setOpaque(false);
        s.setEditor(editor);

        BordeRedondeado borde = new BordeRedondeado();
        s.setBorder(borde);
        s.setBackground(TARJETA);
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                borde.foco = true;
                s.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                borde.foco = false;
                s.repaint();
            }
        });
        s.addMouseWheelListener(e -> {
            Object nuevo = e.getWheelRotation() < 0 ? s.getNextValue() : s.getPreviousValue();
            if (nuevo != null) {
                s.setValue(nuevo);
            }
        });
        return s;
    }

    /** Botón redondeado: con degradado si es primario, blanco con borde si no. */
    static JButton boton(String texto, boolean primario) {
        JButton b = new JButton(texto) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                suavizado(g2);
                if (!isEnabled()) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
                }
                RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5,
                        getHeight() - 1.5, 14, 14);
                boolean encima = getModel().isRollover() && isEnabled();
                if (primario) {
                    Color a = getModel().isPressed() ? ACENTO.darker() : ACENTO;
                    Color b2 = encima ? ACENTO_2.brighter() : ACENTO_2;
                    g2.setPaint(new GradientPaint(0, 0, a, getWidth(), getHeight(), b2));
                    g2.fill(forma);
                } else {
                    g2.setColor(getModel().isPressed() ? new Color(0xEEF2FF) : TARJETA);
                    g2.fill(forma);
                    g2.setColor(encima ? ACENTO : BORDE);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(forma);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(fuente(Font.BOLD, 13f));
        b.setForeground(primario ? Color.WHITE : TEXTO);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Botón pequeño con una "×" para quitar una fila de una lista. */
    static JButton botonQuitar(String ayuda) {
        JButton b = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                suavizado(g2);
                boolean encima = getModel().isRollover();
                if (encima) {
                    g2.setColor(new Color(0xFEE2E2));
                    g2.fill(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                }
                g2.setColor(encima ? Tema.ERROR : TEXTO_SUAVE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                g2.draw(new java.awt.geom.Line2D.Double(cx - 4, cy - 4, cx + 4, cy + 4));
                g2.draw(new java.awt.geom.Line2D.Double(cx - 4, cy + 4, cx + 4, cy - 4));
                g2.dispose();
            }
        };
        b.setToolTipText(ayuda);
        b.setRolloverEnabled(true);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(28, 28));
        return b;
    }

    private static JButton flecha(boolean arriba) {
        JButton b = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                suavizado(g2);
                g2.setColor(getModel().isRollover() ? ACENTO : TEXTO_SUAVE);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0 + (arriba ? 1 : -1);
                double d = arriba ? 2 : -2;
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 3.5, cy + d * 0.75);
                p.lineTo(cx, cy - d);
                p.lineTo(cx + 3.5, cy + d * 0.75);
                g2.draw(p);
                g2.dispose();
            }
        };
        b.setRolloverEnabled(true);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(20, 12));
        return b;
    }

    /** Campo de texto con el mismo estilo que los campos numéricos. */
    static JTextField campoTexto(int columnas) {
        JTextField t = new JTextField(columnas);
        t.setFont(fuente(Font.PLAIN, 14f));
        t.setForeground(TEXTO);
        t.setBackground(TARJETA);
        t.setCaretColor(ACENTO);
        BordeRedondeado borde = new BordeRedondeado(new Insets(8, 12, 8, 12));
        t.setBorder(borde);
        t.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                borde.foco = true;
                t.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                borde.foco = false;
                t.repaint();
            }
        });
        return t;
    }

    /**
     * Reescala una imagen con buena calidad, reduciéndola a la mitad por pasos
     * cuando la diferencia de tamaño es grande.
     */
    static BufferedImage escalar(BufferedImage src, int w, int h) {
        BufferedImage actual = src;
        while (actual.getWidth() / 2 >= w && actual.getHeight() / 2 >= h) {
            actual = redibujar(actual, actual.getWidth() / 2, actual.getHeight() / 2);
        }
        return redibujar(actual, w, h);
    }

    private static BufferedImage redibujar(BufferedImage src, int w, int h) {
        BufferedImage destino = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = destino.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return destino;
    }

    /** Miniatura cuadrada con esquinas redondeadas, recortando la imagen por el centro. */
    static BufferedImage miniatura(BufferedImage src, int tam) {
        double escala = Math.max((double) tam / src.getWidth(), (double) tam / src.getHeight());
        BufferedImage escalada = escalar(src, (int) Math.ceil(src.getWidth() * escala),
                (int) Math.ceil(src.getHeight() * escala));

        BufferedImage m = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = m.createGraphics();
        suavizado(g2);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(0, 0, tam, tam, tam / 3.0, tam / 3.0));
        g2.setComposite(java.awt.AlphaComposite.SrcIn);
        g2.drawImage(escalada, (tam - escalada.getWidth()) / 2, (tam - escalada.getHeight()) / 2, null);
        g2.dispose();
        return m;
    }

    /** Borde redondeado que se resalta con el color de acento al tener el foco. */
    static final class BordeRedondeado extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Insets margen;
        boolean foco;

        BordeRedondeado() {
            this(new Insets(6, 10, 6, 4));
        }

        BordeRedondeado(Insets margen) {
            this.margen = margen;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizado(g2);
            g2.setColor(foco ? ACENTO : BORDE);
            g2.setStroke(new BasicStroke(foco ? 2f : 1.2f));
            g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2.5, h - 2.5, 12, 12));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return (Insets) margen.clone();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets i = getBorderInsets(c);
            insets.set(i.top, i.left, i.bottom, i.right);
            return insets;
        }
    }

    /** Cuadrado redondeado de color con un punto central, usado en títulos y leyendas. */
    static final class IconoPunto implements Icon {
        private final Color color;
        private final int tamano;

        IconoPunto(Color color, int tamano) {
            this.color = color;
            this.tamano = tamano;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizado(g2);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
            g2.fill(new RoundRectangle2D.Double(x, y, tamano, tamano, tamano / 2.5, tamano / 2.5));
            g2.setColor(color);
            double d = tamano * 0.4;
            g2.fill(new java.awt.geom.Ellipse2D.Double(x + (tamano - d) / 2, y + (tamano - d) / 2, d, d));
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }

    /**
     * Logotipo: una impresora 3D (marco, carro con extrusor, cama y una pieza
     * a medio imprimir) sobre un degradado. Se dibuja en una cuadrícula de
     * 100x100 y se escala, por lo que se ve nítido a cualquier tamaño.
     */
    static final class IconoLogo implements Icon {
        private static final Color FILAMENTO = new Color(0xFBBF24);
        private static final Color FILAMENTO_OSCURO = new Color(0xF59E0B);

        private final int tamano;

        IconoLogo(int tamano) {
            this.tamano = tamano;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizado(g2);
            g2.translate(x, y);
            g2.scale(tamano / 100.0, tamano / 100.0);

            // Fondo
            g2.setPaint(new GradientPaint(0, 0, ACENTO, 100, 100, ACENTO_2));
            g2.fill(new RoundRectangle2D.Double(0, 0, 100, 100, 30, 30));

            // Marco de la impresora (dos columnas y travesaño superior)
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D marco = new Path2D.Double();
            marco.moveTo(22, 78);
            marco.lineTo(22, 20);
            marco.lineTo(78, 20);
            marco.lineTo(78, 78);
            g2.draw(marco);

            // Eje X por el que se mueve el carro
            g2.setColor(new Color(255, 255, 255, 190));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Line2D.Double(22, 36, 78, 36));

            // Carro del extrusor y boquilla
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(39, 29, 22, 15, 5, 5));
            Path2D boquilla = new Path2D.Double();
            boquilla.moveTo(44, 43);
            boquilla.lineTo(56, 43);
            boquilla.lineTo(50, 51);
            boquilla.closePath();
            g2.fill(boquilla);

            // Filamento caliente saliendo de la boquilla
            g2.setColor(FILAMENTO);
            g2.fill(new java.awt.geom.Ellipse2D.Double(47.5, 50.5, 5, 5));

            // Pieza impresa capa a capa
            double[][] capas = { { 33, 70, 34 }, { 36, 64, 28 }, { 39, 58, 22 } };
            for (int i = 0; i < capas.length; i++) {
                g2.setColor(i % 2 == 0 ? FILAMENTO : FILAMENTO_OSCURO);
                g2.fill(new RoundRectangle2D.Double(capas[i][0], capas[i][1], capas[i][2], 5.2, 3, 3));
            }

            // Cama de impresión
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(14, 76, 72, 8, 6, 6));

            g2.dispose();
        }

        /** Varias resoluciones para el icono de la ventana y la barra de tareas. */
        static java.util.List<java.awt.Image> imagenesVentana() {
            java.util.List<java.awt.Image> lista = new java.util.ArrayList<>();
            for (int t : new int[] { 16, 20, 24, 32, 40, 48, 64, 128, 256 }) {
                BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                new IconoLogo(t).paintIcon(null, g, 0, 0);
                g.dispose();
                lista.add(img);
            }
            return lista;
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }
}
