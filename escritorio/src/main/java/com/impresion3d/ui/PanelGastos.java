package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.impresion3d.modelo.GastoAdicional;

/**
 * Lista editable de gastos adicionales: cada fila tiene concepto, importe y,
 * opcionalmente, si se cobra por pedido o por unidad.
 */
class PanelGastos extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String POR_PEDIDO = "por pedido";
    private static final String POR_UNIDAD = "por unidad";

    private final JPanel filas = new JPanel(new GridBagLayout());
    private final List<Fila> lista = new ArrayList<>();
    private final JLabel lblVacio;
    private final boolean conTipo;
    private final transient Runnable alCambiar;

    /**
     * @param alCambiar  se llama cada vez que cambia algún gasto
     * @param conTipo    {@code true} para poder elegir "por pedido" o "por unidad";
     *                   si es {@code false}, todos los gastos son por unidad
     * @param textoVacio texto que se muestra cuando no hay gastos
     * @param altoLista  altura de la lista antes de que aparezca la barra de desplazamiento
     */
    PanelGastos(Runnable alCambiar, boolean conTipo, String textoVacio, int altoLista) {
        super(new BorderLayout(0, 10));
        this.alCambiar = alCambiar;
        this.conTipo = conTipo;
        this.lblVacio = Tema.etiqueta(textoVacio, Font.ITALIC, 13f, Tema.TEXTO_SUAVE);
        setOpaque(false);
        filas.setOpaque(false);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(filas, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(contenedor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(100, altoLista));

        JButton btnAnadir = Tema.boton("+ Añadir gasto", false);
        btnAnadir.addActionListener(e -> {
            anadirFila(new GastoAdicional("", 0, !conTipo));
            lista.get(lista.size() - 1).concepto.requestFocusInWindow();
            alCambiar.run();
        });
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(btnAnadir, BorderLayout.WEST);

        add(scroll, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        reconstruir();
    }

    List<GastoAdicional> getGastos() {
        List<GastoAdicional> gastos = new ArrayList<>();
        for (Fila f : lista) {
            gastos.add(new GastoAdicional(f.concepto.getText(),
                    ((Number) f.importe.getValue()).doubleValue(),
                    !conTipo || POR_UNIDAD.equals(f.tipo.getSelectedItem())));
        }
        return gastos;
    }

    void setGastos(List<GastoAdicional> gastos) {
        lista.clear();
        for (GastoAdicional g : gastos) {
            anadirFila(g);
        }
        reconstruir();
    }

    private void anadirFila(GastoAdicional g) {
        Fila f = new Fila(g);
        lista.add(f);
        reconstruir();
    }

    private void quitarFila(Fila f) {
        lista.remove(f);
        reconstruir();
        alCambiar.run();
    }

    /** Vuelve a colocar todas las filas (tras añadir o quitar alguna). */
    private void reconstruir() {
        filas.removeAll();
        if (lista.isEmpty()) {
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            c.insets = new Insets(6, 2, 6, 0);
            filas.add(lblVacio, c);
        } else {
            String[] cabecera = conTipo ? new String[] { "Concepto", "Importe", "Se cobra" }
                    : new String[] { "Concepto", "€ por pieza" };
            for (int col = 0; col < cabecera.length; col++) {
                JLabel l = Tema.etiqueta(cabecera[col], Font.BOLD, 12f, Tema.TEXTO_SUAVE);
                filas.add(l, restriccion(col, 0));
            }
            int colQuitar = cabecera.length;
            for (int i = 0; i < lista.size(); i++) {
                Fila f = lista.get(i);
                filas.add(f.concepto, restriccion(0, i + 1));
                filas.add(f.importe, restriccion(1, i + 1));
                if (conTipo) {
                    filas.add(f.tipo, restriccion(2, i + 1));
                }
                filas.add(f.quitar, restriccion(colQuitar, i + 1));
            }
        }
        filas.revalidate();
        filas.repaint();
    }

    private static GridBagConstraints restriccion(int col, int fila) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = fila;
        c.insets = new Insets(fila == 0 ? 0 : 4, 0, 4, col == 3 ? 0 : 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = col == 3 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        c.weightx = col == 0 ? 1 : 0;
        return c;
    }

    /** Componentes de una fila de la lista. */
    private final class Fila {
        final JTextField concepto = Tema.campoTexto(12);
        final JSpinner importe = Tema.campoNumerico(0, 0, 100000, 0.5);
        final JComboBox<String> tipo = new JComboBox<>(new String[] { POR_PEDIDO, POR_UNIDAD });
        final JButton quitar = Tema.botonQuitar("Quitar este gasto");

        Fila(GastoAdicional g) {
            concepto.setText(g.concepto());
            concepto.setToolTipText("Por ejemplo: envío, embalaje, diseño 3D…");
            importe.setValue(g.importe());
            ((JSpinner.DefaultEditor) importe.getEditor()).getTextField().setColumns(conTipo ? 4 : 5);
            // Con el desplegable "Se cobra" la fila es más ancha: el concepto empieza
            // estrecho y se queda con el espacio que sobre, para que todo quepa.
            if (conTipo) {
                concepto.setColumns(5);
                concepto.setMinimumSize(concepto.getPreferredSize());
            }
            tipo.setSelectedItem(g.porUnidad() ? POR_UNIDAD : POR_PEDIDO);
            tipo.setFont(Tema.fuente(Font.PLAIN, 13f));
            tipo.setBackground(Tema.TARJETA);
            ((JLabel) tipo.getRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

            importe.addChangeListener(e -> alCambiar.run());
            tipo.addActionListener(e -> alCambiar.run());
            quitar.addActionListener(e -> quitarFila(this));
            concepto.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    alCambiar.run();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    alCambiar.run();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    alCambiar.run();
                }
            });
        }
    }
}
