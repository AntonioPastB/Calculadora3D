package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Ayudas para montar los diálogos con formulario: campos en columna sobre
 * fondo blanco y botones abajo, sin recuadros interiores.
 */
final class Formulario {

    private final JPanel campos = new JPanel(new GridBagLayout());
    private final GridBagConstraints c = new GridBagConstraints();

    Formulario() {
        campos.setOpaque(false);
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
    }

    /** Texto informativo (puede ocupar varias líneas). */
    Formulario nota(String texto, java.awt.Color color) {
        JLabel l = new JLabel("<html><div style='width:380px'>" + texto + "</div></html>");
        l.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        l.setForeground(color);
        c.insets = new Insets(0, 0, 14, 0);
        campos.add(l, c);
        return this;
    }

    /** Título de un grupo de campos. */
    Formulario titulo(String texto) {
        c.insets = new Insets(4, 0, 10, 0);
        campos.add(Tema.etiqueta(texto, Font.BOLD, 14f, Tema.TEXTO), c);
        return this;
    }

    Formulario campo(String etiqueta, JComponent campo) {
        c.insets = new Insets(0, 0, 5, 0);
        campos.add(Tema.etiqueta(etiqueta, Font.BOLD, 12f, Tema.TEXTO_SUAVE), c);
        c.insets = new Insets(0, 0, 12, 0);
        campos.add(campo, c);
        return this;
    }

    /** Coloca los campos y los botones en el diálogo, con Intro y Escape. */
    void montar(JDialog dialogo, JButton cancelar, JButton aceptar) {
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pie.setOpaque(false);
        pie.setBorder(new EmptyBorder(10, 0, 0, 0));
        pie.add(cancelar);
        pie.add(aceptar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(22, 24, 20, 24));
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        dialogo.setContentPane(raiz);

        dialogo.getRootPane().setDefaultButton(aceptar);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(dialogo.getOwner());
    }

    /** Área de texto de varias líneas con el mismo estilo que los demás campos. */
    static JScrollPane areaTexto(JTextArea area, int filas) {
        area.setRows(filas);
        area.setColumns(28);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Tema.fuente(Font.PLAIN, 14f));
        area.setForeground(Tema.TEXTO);
        area.setCaretColor(Tema.ACENTO);
        area.setBackground(Tema.TARJETA);
        area.setBorder(new EmptyBorder(0, 0, 0, 0));
        // Tab pasa al siguiente campo en lugar de escribir un tabulador.
        area.setFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        JScrollPane scroll = new JScrollPane(area);
        Tema.BordeRedondeado borde = new Tema.BordeRedondeado(new Insets(8, 12, 8, 12));
        scroll.setBorder(borde);
        scroll.setBackground(Tema.TARJETA);
        scroll.getViewport().setBackground(Tema.TARJETA);
        area.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                borde.foco = true;
                scroll.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                borde.foco = false;
                scroll.repaint();
            }
        });
        return scroll;
    }
}
