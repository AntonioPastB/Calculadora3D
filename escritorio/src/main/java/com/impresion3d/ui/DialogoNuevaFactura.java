package com.impresion3d.ui;

import java.awt.Window;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.impresion3d.modelo.Cliente;
import com.impresion3d.modelo.Emisor;
import com.impresion3d.modelo.Textos;

/** Pide los datos del cliente para emitir una factura a partir de un presupuesto. */
class DialogoNuevaFactura extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Textos.ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtNombre = Tema.campoTexto(28);
    private final JTextField txtNif = Tema.campoTexto(14);
    private final JTextArea txtDireccion = new JTextArea();
    private final JTextField txtEmail = Tema.campoTexto(28);
    private Cliente resultado;

    /**
     * @param clienteSugerido nombre propuesto (por ejemplo, el del presupuesto)
     * @param total           total con IVA de la factura, para mostrarlo
     */
    DialogoNuevaFactura(Window owner, String numero, LocalDate fecha, Emisor emisor, String clienteSugerido,
            double total) {
        super(owner, "Emitir factura " + numero, ModalityType.APPLICATION_MODAL);
        txtNombre.setText(clienteSugerido);

        JButton btnCancelar = Tema.boton("Cancelar", false);
        JButton btnEmitir = Tema.boton("Emitir factura", true);
        btnCancelar.addActionListener(e -> dispose());
        btnEmitir.addActionListener(e -> {
            Cliente c = new Cliente(txtNombre.getText(), txtNif.getText(), txtDireccion.getText(), txtEmail.getText());
            if (c.nombre().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe el nombre o la razón social del cliente.",
                        "Emitir factura", JOptionPane.WARNING_MESSAGE);
                txtNombre.requestFocusInWindow();
                return;
            }
            if ((c.nif().isEmpty() || c.direccion().isEmpty()) && JOptionPane.showConfirmDialog(this,
                    "Sin el NIF y la dirección del cliente la factura será válida solo como factura\n"
                            + "simplificada (para particulares e importes pequeños). ¿Emitirla así?",
                    "Emitir factura", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Se emitirá la factura nº " + numero + " por "
                    + EUROS.format(total) + ".\nUna vez emitida no se puede modificar. ¿Continuar?",
                    "Emitir factura", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            resultado = c;
            dispose();
        });

        new Formulario()
                .nota("<b>Factura nº " + numero + "</b> · " + fecha.format(FECHA) + " · <b>" + EUROS.format(total)
                        + "</b> IVA incluido<br>Emisor: " + emisor.nombre() + " (" + emisor.nif() + ")",
                        Tema.TEXTO)
                .titulo("Datos del cliente")
                .campo("Nombre o razón social *", txtNombre)
                .campo("NIF / CIF", txtNif)
                .campo("Dirección", Formulario.areaTexto(txtDireccion, 3))
                .campo("Email (opcional)", txtEmail)
                .montar(this, btnCancelar, btnEmitir);
    }

    /** Muestra el diálogo y devuelve el cliente, o {@code null} si se ha cancelado. */
    Cliente mostrar() {
        setVisible(true);
        return resultado;
    }
}
