package com.impresion3d.ui;

import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.impresion3d.modelo.Emisor;

/** "Mis datos fiscales": los datos del emisor que aparecen en presupuestos y facturas. */
class DialogoEmisor extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtNombre = Tema.campoTexto(28);
    private final JTextField txtNif = Tema.campoTexto(14);
    private final JTextArea txtDireccion = new JTextArea();
    private final JTextField txtEmail = Tema.campoTexto(28);
    private final JTextField txtTelefono = Tema.campoTexto(14);
    private Emisor resultado;

    DialogoEmisor(Window owner, Emisor actual) {
        super(owner, "Mis datos fiscales", ModalityType.APPLICATION_MODAL);
        txtNombre.setText(actual.nombre());
        txtNif.setText(actual.nif());
        txtDireccion.setText(actual.direccion());
        txtEmail.setText(actual.email());
        txtTelefono.setText(actual.telefono());

        JButton btnCancelar = Tema.boton("Cancelar", false);
        JButton btnGuardar = Tema.boton("Guardar", true);
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> {
            Emisor e2 = new Emisor(txtNombre.getText(), txtNif.getText(), txtDireccion.getText(),
                    txtEmail.getText(), txtTelefono.getText());
            if (!e2.vacio() && !e2.completoParaFacturar() && JOptionPane.showConfirmDialog(this,
                    "Para poder emitir facturas hacen falta el nombre, el NIF y la dirección.\n"
                            + "¿Guardar de todas formas?", "Mis datos fiscales", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
            resultado = e2;
            dispose();
        });

        new Formulario()
                .nota("Aparecen en tus presupuestos y facturas. El nombre, el NIF y la dirección son "
                        + "obligatorios para emitir facturas.", Tema.TEXTO_SUAVE)
                .campo("Nombre y apellidos o razón social *", txtNombre)
                .campo("NIF / CIF *", txtNif)
                .campo("Dirección completa (calle, código postal, ciudad) *", Formulario.areaTexto(txtDireccion, 3))
                .campo("Email (opcional)", txtEmail)
                .campo("Teléfono (opcional)", txtTelefono)
                .montar(this, btnCancelar, btnGuardar);
    }

    /** Muestra el diálogo y devuelve los datos guardados, o {@code null} si se ha cancelado. */
    Emisor mostrar() {
        setVisible(true);
        return resultado;
    }
}
