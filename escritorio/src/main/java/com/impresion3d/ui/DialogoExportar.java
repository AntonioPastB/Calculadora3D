package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * Pide los datos opcionales del presupuesto (cliente y descripción de la
 * pieza) y el formato en que se quiere exportar.
 */
class DialogoExportar extends JDialog {

    private static final long serialVersionUID = 1L;

    enum Formato {
        PDF("pdf", "Documento PDF"), CSV("csv", "Hoja de cálculo CSV");

        final String extension;
        final String descripcion;

        Formato(String extension, String descripcion) {
            this.extension = extension;
            this.descripcion = descripcion;
        }
    }

    record Resultado(String cliente, Formato formato) {
    }

    private static String ultimoCliente = "";
    private static Formato ultimoFormato = Formato.PDF;

    private final JTextField campoCliente = Tema.campoTexto(26);
    private final JRadioButton rbPdf = opcion("PDF · presupuesto para enviar al cliente");
    private final JRadioButton rbCsv = opcion("CSV · para abrir con Excel");
    private Resultado resultado;

    /** @param clienteSugerido cliente que se propone (por ejemplo, el nombre del presupuesto) */
    DialogoExportar(Window owner, String clienteSugerido) {
        super(owner, "Exportar presupuesto", ModalityType.APPLICATION_MODAL);

        campoCliente.setText(clienteSugerido == null || clienteSugerido.isEmpty() ? ultimoCliente : clienteSugerido);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbPdf);
        grupo.add(rbCsv);
        (ultimoFormato == Formato.CSV ? rbCsv : rbPdf).setSelected(true);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        etiqueta(campos, c, "Cliente (opcional)");
        campo(campos, c, campoCliente);
        etiqueta(campos, c, "Formato");
        c.insets = new Insets(0, 0, 2, 0);
        campos.add(rbPdf, c);
        c.insets = new Insets(0, 0, 0, 0);
        campos.add(rbCsv, c);

        JButton btnCancelar = Tema.boton("Cancelar", false);
        JButton btnExportar = Tema.boton("Exportar…", true);
        btnCancelar.addActionListener(e -> dispose());
        btnExportar.addActionListener(e -> {
            Formato f = rbCsv.isSelected() ? Formato.CSV : Formato.PDF;
            ultimoCliente = campoCliente.getText().trim();
            ultimoFormato = f;
            resultado = new Resultado(ultimoCliente, f);
            dispose();
        });
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pie.setOpaque(false);
        pie.setBorder(new EmptyBorder(20, 0, 0, 0));
        pie.add(btnCancelar);
        pie.add(btnExportar);

        // Todo va directamente sobre el fondo de la ventana, sin recuadro interior.
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(22, 24, 20, 24));
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        getRootPane().setDefaultButton(btnExportar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Muestra el diálogo y devuelve lo elegido, o {@code null} si se ha cancelado. */
    Resultado mostrar() {
        setVisible(true);
        return resultado;
    }

    private static JRadioButton opcion(String texto) {
        JRadioButton rb = new JRadioButton(texto);
        rb.setOpaque(false);
        rb.setFont(Tema.fuente(Font.PLAIN, 13f));
        rb.setForeground(Tema.TEXTO);
        rb.setFocusPainted(false);
        return rb;
    }

    private static void etiqueta(JPanel p, GridBagConstraints c, String texto) {
        c.insets = new Insets(0, 0, 6, 0);
        p.add(Tema.etiqueta(texto, Font.BOLD, 12f, Tema.TEXTO_SUAVE), c);
    }

    private static void campo(JPanel p, GridBagConstraints c, JComponent campo) {
        c.insets = new Insets(0, 0, 14, 0);
        p.add(campo, c);
    }
}
