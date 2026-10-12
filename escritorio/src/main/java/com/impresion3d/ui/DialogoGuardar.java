package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Pide el nombre (opcional) de un presupuesto, al guardarlo o al renombrarlo
 * desde el historial. Las imágenes van en cada pieza.
 */
class DialogoGuardar extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField campoNombre = Tema.campoTexto(26);
    private String resultado;

    DialogoGuardar(Window owner, String titulo, String nombreInicial, String ayuda) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        campoNombre.setText(nombreInicial);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 0);
        campos.add(Tema.etiqueta("Nombre del presupuesto o del cliente (opcional)", Font.BOLD, 12f,
                Tema.TEXTO_SUAVE), c);
        c.insets = new Insets(0, 0, ayuda == null ? 0 : 10, 0);
        campos.add(campoNombre, c);
        if (ayuda != null) {
            c.insets = new Insets(0, 0, 0, 0);
            campos.add(Tema.etiqueta(ayuda, Font.PLAIN, 12f, Tema.TEXTO_SUAVE), c);
        }

        JButton btnCancelar = Tema.boton("Cancelar", false);
        JButton btnGuardar = Tema.boton("Guardar", true);
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> {
            resultado = campoNombre.getText().trim();
            dispose();
        });
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pie.setOpaque(false);
        pie.setBorder(new EmptyBorder(20, 0, 0, 0));
        pie.add(btnCancelar);
        pie.add(btnGuardar);

        // Todo va directamente sobre el fondo de la ventana, sin recuadro interior.
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(22, 24, 20, 24));
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        // Intro guarda y Escape cancela.
        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Muestra el diálogo y devuelve el nombre escrito, o {@code null} si se ha cancelado. */
    String mostrar() {
        setVisible(true);
        return resultado;
    }
}
