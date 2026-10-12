package com.impresion3d.ui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Utilidades para elegir una imagen con el selector de archivos o arrastrándola. */
final class SelectorImagen {

    private static File ultimaCarpeta;

    private SelectorImagen() {
    }

    /** Abre el selector de archivos y devuelve la imagen elegida, o {@code null}. */
    static File elegir(Component padre) {
        JFileChooser selector = new JFileChooser(ultimaCarpeta);
        selector.setDialogTitle("Elegir imagen");
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG, GIF, BMP)",
                "png", "jpg", "jpeg", "gif", "bmp"));
        if (selector.showOpenDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        ultimaCarpeta = selector.getCurrentDirectory();
        return selector.getSelectedFile();
    }

    /**
     * Permite soltar un archivo sobre el componente. {@code alSoltar} recibe el
     * archivo y devuelve si lo ha aceptado.
     */
    static void permitirArrastrar(JComponent destino, Predicate<File> alSoltar) {
        destino.setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport soporte) {
                return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport soporte) {
                try {
                    List<?> archivos = (List<?>) soporte.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!archivos.isEmpty() && archivos.get(0) instanceof File f) {
                        return alSoltar.test(f);
                    }
                } catch (Exception e) {
                    // Arrastre no válido: se ignora.
                }
                return false;
            }
        });
    }
}
