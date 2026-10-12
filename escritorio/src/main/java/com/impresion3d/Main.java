package com.impresion3d;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.impresion3d.ui.VentanaCalculadora;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Si no se puede aplicar el aspecto del sistema, se usa el predeterminado.
            }
            new VentanaCalculadora().setVisible(true);
        });
    }
}
