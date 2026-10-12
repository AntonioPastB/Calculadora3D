package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.impresion3d.modelo.Factura;
import com.impresion3d.modelo.Textos;

/**
 * Facturas emitidas, de la más reciente a la más antigua. No se pueden
 * modificar; solo se puede eliminar la última de su año, para no dejar huecos
 * en la numeración.
 */
class DialogoFacturas extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Textos.ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLUMNAS = { "Número", "Fecha", "Cliente", "NIF", "Total" };

    private final transient List<Factura> facturas;
    private final Modelo modelo = new Modelo();
    private final JTable tabla = new JTable(modelo);

    /**
     * @param facturas     facturas emitidas (se modifica al eliminar la última)
     * @param alExportar   exporta una factura en el formato elegido
     * @param alCambiar    se llama tras eliminar una factura, para guardar
     * @param alNueva      factura el presupuesto que hay en la calculadora
     * @param alEditarDatos abre "Mis datos fiscales"
     */
    DialogoFacturas(Window owner, List<Factura> facturas, BiConsumer<Factura, DialogoExportar.Formato> alExportar,
            Runnable alCambiar, Runnable alNueva, Runnable alEditarDatos) {
        super(owner, "Facturas emitidas", ModalityType.APPLICATION_MODAL);
        this.facturas = facturas;

        configurarTabla();
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Tema.TARJETA);

        JButton btnDatos = Tema.boton("Mis datos fiscales", false);
        JButton btnNueva = Tema.boton("Facturar el presupuesto actual", false);
        JButton btnEliminar = Tema.boton("Eliminar", false);
        JButton btnCsv = Tema.boton("Exportar CSV", false);
        JButton btnPdf = Tema.boton("Exportar PDF", true);
        JButton btnCerrar = Tema.boton("Cerrar", false);
        btnEliminar.setToolTipText("Solo se puede eliminar la última factura de cada año");

        btnDatos.addActionListener(e -> alEditarDatos.run());
        btnNueva.addActionListener(e -> {
            dispose();
            alNueva.run();
        });
        btnPdf.addActionListener(e -> conSeleccion(f -> alExportar.accept(f, DialogoExportar.Formato.PDF)));
        btnCsv.addActionListener(e -> conSeleccion(f -> alExportar.accept(f, DialogoExportar.Formato.CSV)));
        btnEliminar.addActionListener(e -> conSeleccion(f -> {
            if (JOptionPane.showConfirmDialog(this, "¿Eliminar la factura nº " + f.numero() + "?\n"
                    + "Hazlo solo si no se la has enviado todavía al cliente.", "Eliminar factura",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                facturas.remove(f);
                modelo.fireTableDataChanged();
                alCambiar.run();
            }
        }));
        btnCerrar.addActionListener(e -> dispose());

        JButton[] requierenSeleccion = { btnPdf, btnCsv };
        for (JButton b : requierenSeleccion) {
            b.setEnabled(false);
        }
        btnEliminar.setEnabled(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            Factura f = seleccionada();
            for (JButton b : requierenSeleccion) {
                b.setEnabled(f != null);
            }
            btnEliminar.setEnabled(f != null && Factura.sePuedeEliminar(facturas, f));
        });

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izquierda.setOpaque(false);
        izquierda.add(btnDatos);
        izquierda.add(btnNueva);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(btnEliminar);
        derecha.add(btnCerrar);
        derecha.add(btnCsv);
        derecha.add(btnPdf);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(14, 0, 0, 0)));
        pie.add(izquierda, BorderLayout.WEST);
        pie.add(derecha, BorderLayout.EAST);

        JLabel vacio = Tema.etiqueta("Todavía no has emitido ninguna factura. Abre el Historial y pulsa "
                + "«Convertir en factura» en un presupuesto.", Font.PLAIN, 13f, Tema.TEXTO_SUAVE);
        vacio.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(16, 18, 16, 18));
        raiz.add(facturas.isEmpty() ? vacio : scroll, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        setSize(new Dimension(1060, 480));
        setMinimumSize(new Dimension(900, 360));
        setLocationRelativeTo(owner);
        if (!facturas.isEmpty()) {
            tabla.setRowSelectionInterval(0, 0);
        }
    }

    /** Las facturas se muestran de la más reciente a la más antigua. */
    private Factura enFila(int fila) {
        return facturas.get(facturas.size() - 1 - fila);
    }

    private Factura seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila >= 0 && fila < facturas.size() ? enFila(fila) : null;
    }

    private void conSeleccion(Consumer<Factura> accion) {
        Factura f = seleccionada();
        if (f != null) {
            accion.accept(f);
        }
    }

    private void configurarTabla() {
        tabla.setRowHeight(36);
        tabla.setFont(Tema.fuente(Font.PLAIN, 13f));
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(Tema.BORDE);
        tabla.setSelectionBackground(new Color(0xECFDF5));
        tabla.setSelectionForeground(Tema.TEXTO);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int fila, int col) {
                super.getTableCellRendererComponent(t, v, false, false, fila, col);
                setFont(Tema.fuente(Font.BOLD, 12f));
                setForeground(Tema.TEXTO_SUAVE);
                setBackground(Tema.TARJETA);
                setHorizontalAlignment(col == COLUMNAS.length - 1 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                        new EmptyBorder(8, 10, 8, 10)));
                return this;
            }
        });
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int fila, int col) {
                super.getTableCellRendererComponent(t, v, s, false, fila, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                boolean numero = col == 0;
                boolean total = col == COLUMNAS.length - 1;
                setFont(Tema.fuente(numero || total ? Font.BOLD : Font.PLAIN, 13f));
                setForeground(total ? ExportadorPdf.FACTURA_1 : Tema.TEXTO);
                setHorizontalAlignment(total ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return this;
            }
        });
        int[] anchos = { 110, 100, 360, 130, 120 };
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private final class Modelo extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return facturas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNAS[col];
        }

        @Override
        public Object getValueAt(int fila, int col) {
            Factura f = enFila(fila);
            return switch (col) {
                case 0 -> f.numero();
                case 1 -> f.fecha().format(FECHA);
                case 2 -> f.cliente().nombre();
                case 3 -> f.cliente().nif().isEmpty() ? "—" : f.cliente().nif();
                default -> EUROS.format(f.total());
            };
        }
    }
}
