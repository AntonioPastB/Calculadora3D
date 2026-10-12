package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.impresion3d.modelo.Donaciones;

/** Ventana "Apoya el proyecto", con el botón para donar por PayPal. */
class DialogoDonaciones extends JDialog {

    private static final long serialVersionUID = 1L;

    DialogoDonaciones(Window owner) {
        super(owner, Donaciones.TITULO, ModalityType.APPLICATION_MODAL);

        // Todo va directamente sobre el fondo de la ventana, sin recuadro interior.
        JLabel corazon = new JLabel(new IconoCorazon(44, Tema.C_GASTOS_PIEZA));
        corazon.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel texto = new JLabel("<html><div style='width:330px;text-align:center'>" + Donaciones.TEXTO
                + "</div></html>");
        texto.setFont(Tema.fuente(Font.PLAIN, 14f));
        texto.setForeground(Tema.TEXTO);
        texto.setHorizontalAlignment(SwingConstants.CENTER);
        texto.setBorder(new EmptyBorder(14, 0, 0, 0));

        JLabel nota = Tema.etiqueta("Se abrirá PayPal en tu navegador.", Font.PLAIN, 12f, Tema.TEXTO_SUAVE);
        nota.setHorizontalAlignment(SwingConstants.CENTER);
        nota.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setOpaque(false);
        cuerpo.add(corazon, BorderLayout.NORTH);
        cuerpo.add(texto, BorderLayout.CENTER);
        cuerpo.add(nota, BorderLayout.SOUTH);

        JButton btnCerrar = Tema.boton("Ahora no", false);
        JButton btnDonar = Tema.boton("Donar con PayPal", true);
        btnDonar.setIcon(new IconoCorazon(14, java.awt.Color.WHITE));
        btnDonar.setIconTextGap(8);
        btnCerrar.addActionListener(e -> dispose());
        btnDonar.addActionListener(e -> abrirEnlace());

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pie.setOpaque(false);
        pie.setBorder(new EmptyBorder(20, 0, 0, 0));
        pie.add(btnCerrar);
        pie.add(btnDonar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(24, 28, 22, 28));
        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        getRootPane().setDefaultButton(btnDonar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void abrirEnlace() {
        if (!Donaciones.estaConfigurado()) {
            JOptionPane.showMessageDialog(this, "El enlace de donaciones todavía no está configurado.",
                    Donaciones.TITULO, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(Donaciones.ENLACE));
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se ha podido abrir el navegador.\nPuedes donar en:\n"
                    + Donaciones.ENLACE, Donaciones.TITULO, JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Corazón relleno del tamaño y color indicados. */
    static final class IconoCorazon implements Icon {
        private final int tam;
        private final java.awt.Color color;

        IconoCorazon(int tam, java.awt.Color color) {
            this.tam = tam;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizado(g2);
            g2.translate(x, y + tam * 0.06);
            g2.scale(tam, tam);
            Path2D p = new Path2D.Double();
            p.moveTo(0.5, 0.92);
            p.curveTo(0.15, 0.68, 0.0, 0.48, 0.0, 0.3);
            p.curveTo(0.0, 0.12, 0.14, 0.0, 0.29, 0.0);
            p.curveTo(0.39, 0.0, 0.46, 0.06, 0.5, 0.14);
            p.curveTo(0.54, 0.06, 0.61, 0.0, 0.71, 0.0);
            p.curveTo(0.86, 0.0, 1.0, 0.12, 1.0, 0.3);
            p.curveTo(1.0, 0.48, 0.85, 0.68, 0.5, 0.92);
            p.closePath();
            g2.setColor(color);
            g2.fill(p);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tam;
        }

        @Override
        public int getIconHeight() {
            return tam;
        }
    }
}
