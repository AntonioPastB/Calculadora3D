package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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
import javax.swing.table.JTableHeader;

import com.impresion3d.modelo.Pieza;
import com.impresion3d.modelo.Presupuesto;
import com.impresion3d.modelo.Textos;
import com.impresion3d.persistencia.ImagenesEscritorio;

/**
 * Ventana que muestra los presupuestos guardados y permite cargarlos,
 * renombrarlos o eliminarlos.
 */
class DialogoHistorial extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Textos.ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLUMNAS = { "", "Nombre", "Fecha", "Piezas", "Uds", "Total" };
    private static final int TAM_MINIATURA = 42;
    private static final Icon SIN_MINIATURA = new IconoSinImagen(TAM_MINIATURA);

    private final transient ImagenesEscritorio almacen;
    private final transient List<Presupuesto> historial;
    private final transient Runnable alCambiar;
    private final transient Map<String, BufferedImage> imagenes = new HashMap<>();
    private final transient Map<String, Icon> miniaturas = new HashMap<>();

    private final ModeloTabla modelo = new ModeloTabla();
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = Tema.etiqueta("", Font.PLAIN, 13f, Tema.TEXTO_SUAVE);

    private final PanelImagen detalleImagen = new PanelImagen("Selecciona un presupuesto", 250, 190);
    private final JLabel detalleNombre = Tema.etiqueta(" ", Font.BOLD, 16f, Tema.TEXTO);
    private final JLabel detalleInfo = Tema.etiqueta(" ", Font.PLAIN, 12f, Tema.TEXTO_SUAVE);
    private final JLabel detallePrecio = Tema.etiqueta(" ", Font.BOLD, 26f, Tema.ACENTO);

    /**
     * @param historial lista que se muestra; se modifica al renombrar o eliminar entradas
     * @param alCargar  se llama con el presupuesto elegido para cargarlo en la calculadora
     * @param alCambiar  se llama después de renombrar o eliminar entradas, para guardar el historial
     * @param alFacturar se llama con el presupuesto elegido para emitir su factura
     */
    DialogoHistorial(Window owner, ImagenesEscritorio almacen, List<Presupuesto> historial,
            Consumer<Presupuesto> alCargar, Runnable alCambiar, Consumer<Presupuesto> alFacturar) {
        super(owner, "Historial de presupuestos", ModalityType.APPLICATION_MODAL);
        this.almacen = almacen;
        this.historial = historial;
        this.alCambiar = alCambiar;

        configurarTabla();
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Tema.TARJETA);


        JButton btnCargar = Tema.boton("Cargar en la calculadora", true);
        JButton btnEditar = Tema.boton("Renombrar", false);
        JButton btnEliminar = Tema.boton("Eliminar", false);
        JButton btnCerrar = Tema.boton("Cerrar", false);
        JButton btnFacturar = Tema.boton("Convertir en factura", false);
        btnFacturar.setToolTipText("Emite una factura con las piezas e importes de este presupuesto");
        btnFacturar.addActionListener(e -> {
            Presupuesto p = seleccionado();
            if (p != null) {
                dispose();
                alFacturar.accept(p);
            }
        });
        JButton[] requierenSeleccion = { btnCargar, btnEditar, btnEliminar, btnFacturar };
        for (JButton b : requierenSeleccion) {
            b.setEnabled(false);
        }
        tabla.getSelectionModel().addListSelectionListener(e -> {
            boolean haySeleccion = tabla.getSelectedRow() >= 0;
            for (JButton b : requierenSeleccion) {
                b.setEnabled(haySeleccion);
            }
            actualizarDetalle();
        });

        Runnable cargar = () -> {
            Presupuesto p = seleccionado();
            if (p != null) {
                alCargar.accept(p);
                dispose();
            }
        };
        btnCargar.addActionListener(e -> cargar.run());
        btnEditar.addActionListener(e -> editarSeleccionado());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        btnCerrar.addActionListener(e -> dispose());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    cargar.run();
                }
            }
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnEliminar);
        botones.add(btnEditar);
        botones.add(btnCerrar);
        botones.add(btnFacturar);
        botones.add(btnCargar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(14, 4, 0, 0)));
        pie.add(lblResumen, BorderLayout.WEST);
        pie.add(botones, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(0, 0));
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(0, 0, 14, 0));
        centro.add(scroll, BorderLayout.CENTER);
        centro.add(panelDetalle(), BorderLayout.EAST);

        // Todo va directamente sobre el fondo de la ventana, sin recuadros interiores.
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.setBorder(new EmptyBorder(16, 18, 16, 18));
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        actualizarResumen();
        setSize(new Dimension(1220, 580));
        setMinimumSize(new Dimension(900, 450));
        setLocationRelativeTo(owner);
        if (!historial.isEmpty()) {
            tabla.setRowSelectionInterval(0, 0);
        }
    }

    /** Vista previa del presupuesto seleccionado, separada de la tabla por una línea. */
    private JPanel panelDetalle() {
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        cuerpo.add(detalleImagen, c);
        c.insets = new Insets(0, 0, 2, 0);
        cuerpo.add(detalleNombre, c);
        c.insets = new Insets(0, 0, 10, 0);
        cuerpo.add(detalleInfo, c);
        c.insets = new Insets(0, 0, 0, 0);
        cuerpo.add(detallePrecio, c);
        c.weighty = 1;
        cuerpo.add(new JLabel(), c);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Tema.BORDE), new EmptyBorder(4, 20, 0, 4)));
        p.add(cuerpo, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(300, 0));
        return p;
    }

    private void configurarTabla() {
        tabla.setRowHeight(TAM_MINIATURA + 14);
        tabla.setFont(Tema.fuente(Font.PLAIN, 13f));
        tabla.setForeground(Tema.TEXTO);
        tabla.setBackground(Tema.TARJETA);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(Tema.BORDE);
        tabla.setSelectionBackground(new Color(0xEEF2FF));
        tabla.setSelectionForeground(Tema.TEXTO);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);

        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setReorderingAllowed(false);
        cabecera.setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                    boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(t, valor, false, false, fila, col);
                setFont(Tema.fuente(Font.BOLD, 12f));
                setForeground(Tema.TEXTO_SUAVE);
                setBackground(Tema.TARJETA);
                setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                        new EmptyBorder(8, 10, 8, 10)));
                return this;
            }
        });

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                    boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(t, valor, sel, false, fila, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                boolean precio = col == COLUMNAS.length - 1;
                boolean sinNombre = col == 1 && historial.get(fila).nombre().isEmpty();
                int estilo = sinNombre ? Font.ITALIC : precio || col == 1 ? Font.BOLD : Font.PLAIN;
                setFont(Tema.fuente(estilo, 13f));
                setForeground(precio ? Tema.ACENTO : sinNombre ? Tema.TEXTO_SUAVE : Tema.TEXTO);
                setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return this;
            }
        });

        tabla.setDefaultRenderer(Icon.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                    boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(t, null, sel, false, fila, col);
                setIcon((Icon) valor);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 6, 0, 0));
                return this;
            }
        });

        int[] anchos = { 60, 260, 130, 70, 70, 110 };
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);
        tabla.getColumnModel().getColumn(2).setMinWidth(130);
    }

    private Presupuesto seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila >= 0 ? historial.get(fila) : null;
    }

    /** Imagen de la primera pieza que tenga una. */
    private BufferedImage imagen(Presupuesto p) {
        String nombre = p.pedido().imagenPrincipal();
        return nombre == null ? null : imagenes.computeIfAbsent(nombre, almacen::cargar);
    }

    private Icon miniatura(Presupuesto p) {
        BufferedImage img = imagen(p);
        if (img == null) {
            return SIN_MINIATURA;
        }
        return miniaturas.computeIfAbsent(p.pedido().imagenPrincipal(),
                k -> new ImageIcon(Tema.miniatura(img, TAM_MINIATURA)));
    }

    private void actualizarDetalle() {
        Presupuesto p = seleccionado();
        if (p == null) {
            detalleImagen.setImagen(null);
            detalleImagen.setTextoVacio("Selecciona un presupuesto");
            detalleNombre.setText(" ");
            detalleInfo.setText(" ");
            detallePrecio.setText(" ");
            return;
        }
        detalleImagen.setImagen(imagen(p));
        detalleImagen.setTextoVacio("Sin imagen\nAñádela en la pieza al cargarlo");
        detalleNombre.setText(p.nombreVisible());

        StringBuilder info = new StringBuilder("<html>");
        List<Pieza> piezas = p.pedido().piezas();
        int mostradas = Math.min(4, piezas.size());
        for (int i = 0; i < mostradas; i++) {
            info.append("• ").append(escapar(piezas.get(i).nombreVisible(i))).append(" ×")
                    .append(piezas.get(i).cantidad()).append("<br>");
        }
        if (piezas.size() > mostradas) {
            info.append("• y ").append(piezas.size() - mostradas).append(" más<br>");
        }
        info.append("<br>").append(p.unidades()).append(" unidades · ").append(p.fecha().format(FECHA))
                .append("</html>");
        detalleInfo.setText(info.toString());
        detallePrecio.setText(EUROS.format(p.total()));
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void editarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Presupuesto p = historial.get(fila);
        String nombre = new DialogoGuardar(this, "Renombrar presupuesto", p.nombre(), null).mostrar();
        if (nombre == null) {
            return;
        }
        historial.set(fila, p.conNombre(nombre));
        modelo.fireTableRowsUpdated(fila, fila);
        alCambiar.run();
        actualizarDetalle();
    }

    private void eliminarSeleccionado() {
        Presupuesto p = seleccionado();
        if (p == null) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + p.nombreVisible() + "\" del historial?", "Eliminar presupuesto",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            // Las imágenes que ya no use nadie las borra la ventana principal al guardar.
            historial.remove(p);
            modelo.fireTableDataChanged();
            alCambiar.run();
            actualizarResumen();
            actualizarDetalle();
        }
    }

    private void actualizarResumen() {
        int n = historial.size();
        lblResumen.setText(n == 0 ? "Todavía no has guardado ningún presupuesto."
                : n + (n == 1 ? " presupuesto guardado" : " presupuestos guardados")
                        + " · doble clic para cargar");
    }

    private final class ModeloTabla extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return historial.size();
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
        public Class<?> getColumnClass(int col) {
            return col == 0 ? Icon.class : Object.class;
        }

        @Override
        public Object getValueAt(int fila, int col) {
            Presupuesto p = historial.get(fila);
            return switch (col) {
                case 0 -> miniatura(p);
                case 1 -> p.nombreVisible();
                case 2 -> p.fecha().format(FECHA);
                case 3 -> Integer.toString(p.pedido().piezas().size());
                case 4 -> Integer.toString(p.unidades());
                default -> EUROS.format(p.total());
            };
        }
    }

    /** Hueco gris para los cálculos sin imagen. */
    private static final class IconoSinImagen implements Icon {
        private final int tam;

        IconoSinImagen(int tam) {
            this.tam = tam;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            Tema.suavizado(g2);
            g2.setColor(Tema.FONDO);
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, tam, tam, tam / 3.0, tam / 3.0));
            g2.setColor(new Color(0xCBD5E1));
            g2.setStroke(new java.awt.BasicStroke(1.6f));
            double m = tam * 0.28;
            g2.draw(new java.awt.geom.RoundRectangle2D.Double(x + m, y + m + 2, tam - 2 * m, tam - 2 * m - 4, 4, 4));
            g2.fill(new java.awt.geom.Ellipse2D.Double(x + tam / 2.0 + 2, y + m + 5, 4, 4));
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
