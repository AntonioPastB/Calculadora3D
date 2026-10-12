package com.impresion3d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.impresion3d.modelo.CalculadoraPrecio;
import com.impresion3d.modelo.Cliente;
import com.impresion3d.modelo.Emisor;
import com.impresion3d.modelo.Factura;
import com.impresion3d.modelo.DesglosePrecio;
import com.impresion3d.modelo.DocumentoPedido;
import com.impresion3d.modelo.GastoAdicional;
import com.impresion3d.modelo.Material;
import com.impresion3d.modelo.Pedido;
import com.impresion3d.modelo.Pieza;
import com.impresion3d.modelo.Presupuesto;
import com.impresion3d.modelo.ResumenPedido;
import com.impresion3d.modelo.Taller;
import com.impresion3d.modelo.Textos;
import com.impresion3d.modelo.TramoDescuento;
import com.impresion3d.persistencia.Almacen;
import com.impresion3d.persistencia.ExportadorCsv;
import com.impresion3d.persistencia.ImagenesEscritorio;

/**
 * Ventana principal. A la izquierda, tres pestañas: las piezas del presupuesto
 * (se edita una cada vez), los datos comunes del taller y el pedido (descuentos,
 * gastos y resumen). A la derecha, el total y el coste de una unidad de la
 * pieza seleccionada, que se recalculan automáticamente.
 */
public class VentanaCalculadora extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Textos.ES);
    private static final String[] CONCEPTOS = { "Material", "Electricidad", "Amortización",
            "Mano de obra", "Riesgo de fallos", "Gastos de la pieza" };
    private static final Color[] COLORES = { Tema.C_MATERIAL, Tema.C_ELECTRICIDAD,
            Tema.C_AMORTIZACION, Tema.C_MANO_OBRA, Tema.C_FALLOS, Tema.C_GASTOS_PIEZA };

    // ---- Pieza seleccionada
    private final JTextField txtNombre = Tema.campoTexto(14);
    private final JSpinner spUnidades = Tema.campoNumerico(1, 1, 1000000, 1);
    private final JComboBox<Material> cbMaterial = new JComboBox<>(Material.values());
    private final JSpinner spPrecioKg = Tema.campoNumerico(20, 0, 10000, 0.5);
    private final JSpinner spPeso = Tema.campoNumerico(50, 0, 100000, 1);
    private final JSpinner spHoras = Tema.campoNumerico(3, 0, 10000, 1);
    private final JSpinner spMinutos = Tema.campoNumerico(0, 0, 59, 5);
    private final JSpinner spMinPreparacion = Tema.campoNumerico(15, 0, 100000, 5);
    private final PanelGastos panelGastosPieza = new PanelGastos(this::piezaEditada, false,
            "Sin gastos. Añade imanes, insertos, pintura…", 110);
    private final PanelImagen imagenPieza = new PanelImagen("Haz clic o arrastra\nuna imagen", 200, 96);
    private final JButton btnQuitarImagen = Tema.boton("Quitar", false);
    private final SelectorPiezas selector = new SelectorPiezas(this::seleccionarPieza, this::anadirPieza,
            this::duplicarPieza, this::eliminarPieza);
    private final JLabel lblTituloPieza = Tema.etiqueta(" ", Font.BOLD, 13f, Tema.TEXTO_SUAVE);

    // ---- Taller (común a todas las piezas)
    private final JSpinner spPotencia = Tema.campoNumerico(150, 0, 5000, 10);
    private final JSpinner spPrecioKwh = Tema.campoNumerico(0.18, 0, 10, 0.01);
    private final JSpinner spPrecioImpresora = Tema.campoNumerico(400, 0, 100000, 50);
    private final JSpinner spVidaUtil = Tema.campoNumerico(5000, 1, 1000000, 500);
    private final JSpinner spMantenimiento = Tema.campoNumerico(0.05, 0, 100, 0.01);
    private final JSpinner spPrecioHora = Tema.campoNumerico(15, 0, 1000, 1);
    private final JSpinner spFallos = Tema.campoNumerico(10, 0, 99, 1);
    private final JSpinner spMargen = Tema.campoNumerico(30, 0, 1000, 5);
    private final JSpinner spIva = Tema.campoNumerico(21, 0, 100, 1);

    // ---- Pedido
    private final PanelGastos panelGastos = new PanelGastos(this::recalcular, true,
            "Sin gastos adicionales. Añade envío, embalaje, diseño…", 110);
    private final PanelTramos panelTramos = new PanelTramos(this::recalcular);
    private final JPanel lineasResumen = new JPanel(new GridBagLayout());
    private final SelectorPestanas pestanas = new SelectorPestanas();

    // ---- Tarjeta del total
    private final JLabel lblTotal = Tema.etiqueta("-", Font.BOLD, 40f, Color.WHITE);
    private final JLabel lblPorUnidad = Tema.etiqueta(" ", Font.PLAIN, 13f, new Color(255, 255, 255, 220));
    private final JLabel lblSinIva = Tema.etiqueta("-", Font.BOLD, 15f, Color.WHITE);
    private final JLabel lblDescuento = Tema.etiqueta("-", Font.BOLD, 15f, Color.WHITE);
    private final JLabel lblIva = Tema.etiqueta("-", Font.BOLD, 15f, Color.WHITE);

    // ---- Coste de una unidad de la pieza seleccionada
    private final JLabel[] lblImportes = new JLabel[CONCEPTOS.length];
    private final JLabel[] lblPorcentajes = new JLabel[CONCEPTOS.length];
    private final GraficoAnillo grafico = new GraficoAnillo(COLORES);
    private final JLabel lblError = Tema.etiqueta(" ", Font.BOLD, 12f, Tema.ERROR);

    private final JButton btnHistorial = Tema.boton("Historial", false);

    private final transient Almacen almacen = new Almacen();
    private final transient ImagenesEscritorio imagenes = new ImagenesEscritorio(almacen);
    private transient List<Presupuesto> historial = new ArrayList<>();
    /** Facturas emitidas, en orden de emisión. */
    private transient List<Factura> facturas = new ArrayList<>();
    /** Tus datos fiscales para presupuestos y facturas. */
    private transient Emisor emisor = Emisor.VACIO;
    private final JButton btnFacturas = Tema.boton("Facturas", false);
    /** Último precio por kg que el usuario ha usado con cada material. */
    private final Map<Material, Double> preciosMaterial = new EnumMap<>(Material.class);

    /** Piezas del presupuesto que se está editando; siempre hay al menos una. */
    private final transient List<Pieza> piezas = new ArrayList<>();
    private int seleccionada;
    /** Mientras se rellenan los campos de una pieza no se guardan los cambios en ella. */
    private boolean cargando;
    private String ultimoNombre = "";
    private File carpetaExportacion;

    public VentanaCalculadora() {
        super("Calculadora de impresión 3D");
        setIconImages(Tema.IconoLogo.imagenesVentana());

        cbMaterial.setFont(Tema.fuente(Font.PLAIN, 14f));
        cbMaterial.setBackground(Tema.TARJETA);
        panelTramos.setTramos(Pedido.TRAMOS_POR_DEFECTO);
        piezas.add(Pieza.nueva());

        pestanas.anadir("Piezas", paginaPiezas());
        pestanas.anadir("Taller y margen", paginaTaller());
        pestanas.anadir("Pedido", paginaPedido());

        JPanel izquierda = new JPanel(new BorderLayout(0, 10));
        izquierda.setOpaque(false);
        izquierda.add(pestanas.getBarra(), BorderLayout.NORTH);
        izquierda.add(pestanas.getPaginas(), BorderLayout.CENTER);

        JPanel resultados = new JPanel(new BorderLayout(0, 6));
        resultados.setOpaque(false);
        resultados.add(tarjetaPrecioFinal(), BorderLayout.NORTH);
        resultados.add(tarjetaDesglose(), BorderLayout.CENTER);
        resultados.setPreferredSize(new Dimension(370, resultados.getPreferredSize().height));

        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setOpaque(false);
        centro.add(izquierda, BorderLayout.CENTER);
        centro.add(resultados, BorderLayout.EAST);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(new EmptyBorder(20, 20, 20, 20));
        raiz.add(cabecera(), BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                guardarConfiguracion();
                dispose();
                System.exit(0);
            }
        });

        conectarEventos();
        cargarDatosGuardados();
        mostrarPieza();
        recalcular();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void conectarEventos() {
        // Al elegir un material se pone el último precio usado con él o, si no hay,
        // su precio orientativo. "Personalizado" sin precio recordado conserva el actual.
        cbMaterial.addActionListener(e -> {
            Material m = (Material) cbMaterial.getSelectedItem();
            if (m == null || cargando) {
                return;
            }
            Double precio = preciosMaterial.get(m);
            if (precio == null && m.getPrecioKg() >= 0) {
                precio = m.getPrecioKg();
            }
            if (precio != null) {
                spPrecioKg.setValue(precio);
            }
            piezaEditada();
        });
        spPrecioKg.addChangeListener(e -> {
            Material m = (Material) cbMaterial.getSelectedItem();
            if (m != null && !cargando) {
                preciosMaterial.put(m, num(spPrecioKg));
            }
        });
        for (JSpinner s : new JSpinner[] { spUnidades, spPrecioKg, spPeso, spHoras, spMinutos, spMinPreparacion }) {
            s.addChangeListener(e -> piezaEditada());
        }
        txtNombre.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                piezaEditada();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                piezaEditada();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                piezaEditada();
            }
        });
        for (JSpinner s : new JSpinner[] { spPotencia, spPrecioKwh, spPrecioImpresora, spVidaUtil,
                spMantenimiento, spPrecioHora, spFallos, spMargen, spIva }) {
            s.addChangeListener(e -> recalcular());
        }
    }

    // ================================================================ Páginas

    private JComponent paginaPiezas() {
        JPanel tarjetas = new JPanel(new GridBagLayout());
        tarjetas.setOpaque(false);
        // La tarjeta "Pieza" lleva también la imagen, debajo del nombre y las unidades.
        Tarjeta pieza = (Tarjeta) seccion("Pieza", Tema.ACENTO,
                "Nombre (opcional)", txtNombre, "",
                "Unidades", spUnidades, "uds");
        pieza.add(bloqueImagen(), BorderLayout.SOUTH);
        anadirTarjeta(tarjetas, 0, 0, pieza);
        anadirTarjeta(tarjetas, 1, 0, seccion("Material", Tema.C_MATERIAL,
                "Tipo de material", cbMaterial, "",
                "Precio del filamento", spPrecioKg, "€/kg",
                "Peso con soportes", spPeso, "g"));
        anadirTarjeta(tarjetas, 0, 1, seccion("Tiempos por unidad", Tema.C_ELECTRICIDAD,
                "Horas de impresión", spHoras, "h",
                "Minutos de impresión", spMinutos, "min",
                "Preparación y postprocesado", spMinPreparacion, "min"));
        Tarjeta gastos = new Tarjeta("Gastos por unidad", Tema.C_GASTOS_PIEZA);
        gastos.add(panelGastosPieza, BorderLayout.CENTER);
        anadirTarjeta(tarjetas, 1, 1, gastos);
        igualarColumnas(tarjetas);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(selector, BorderLayout.NORTH);
        p.add(tarjetas, BorderLayout.CENTER);
        return p;
    }

    /** Imagen de la pieza (opcional), con el botón para quitarla. */
    private JComponent bloqueImagen() {
        imagenPieza.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        imagenPieza.setToolTipText("Haz clic para elegir una imagen o arrástrala aquí");
        imagenPieza.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                File f = SelectorImagen.elegir(VentanaCalculadora.this);
                if (f != null) {
                    ponerImagen(f);
                }
            }
        });
        SelectorImagen.permitirArrastrar(imagenPieza, this::ponerImagen);
        btnQuitarImagen.addActionListener(e -> {
            piezas.set(seleccionada, piezas.get(seleccionada).conImagen(null));
            mostrarImagen();
            recalcular();
        });

        JPanel lado = new JPanel(new BorderLayout(0, 6));
        lado.setOpaque(false);
        lado.add(Tema.etiqueta("Imagen (opcional)", Font.PLAIN, 13f, Tema.TEXTO_SUAVE), BorderLayout.NORTH);
        lado.add(btnQuitarImagen, BorderLayout.SOUTH);

        JPanel cuerpo = new JPanel(new BorderLayout(10, 0));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(new EmptyBorder(6, 0, 0, 0));
        cuerpo.add(lado, BorderLayout.WEST);
        cuerpo.add(imagenPieza, BorderLayout.CENTER);
        return cuerpo;
    }

    private JComponent paginaTaller() {
        JLabel nota = Tema.etiqueta("Estos datos son comunes a todas las piezas del presupuesto.",
                Font.PLAIN, 13f, Tema.TEXTO_SUAVE);
        nota.setBorder(new EmptyBorder(0, 6, 8, 0));

        JPanel tarjetas = new JPanel(new GridBagLayout());
        tarjetas.setOpaque(false);
        anadirTarjeta(tarjetas, 0, 0, seccion("Energía", Tema.C_ELECTRICIDAD,
                "Consumo de la impresora", spPotencia, "W",
                "Precio de la electricidad", spPrecioKwh, "€/kWh"));
        anadirTarjeta(tarjetas, 1, 0, seccion("Impresora", Tema.C_AMORTIZACION,
                "Precio de la impresora", spPrecioImpresora, "€",
                "Vida útil estimada", spVidaUtil, "h",
                "Mantenimiento y recambios", spMantenimiento, "€/h"));
        anadirTarjeta(tarjetas, 0, 1, seccion("Mano de obra", Tema.C_MANO_OBRA,
                "Precio de la mano de obra", spPrecioHora, "€/h"));
        anadirTarjeta(tarjetas, 1, 1, seccion("Margen, riesgo e IVA", Tema.ACENTO_2,
                "Riesgo de fallos (sobre el coste)", spFallos, "%",
                "Margen de beneficio", spMargen, "%",
                "IVA", spIva, "%"));
        // Empuja las tarjetas hacia arriba.
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = 2;
        relleno.weighty = 1;
        tarjetas.add(new JLabel(), relleno);
        igualarColumnas(tarjetas);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(nota, BorderLayout.NORTH);
        p.add(tarjetas, BorderLayout.CENTER);
        return p;
    }

    private JComponent paginaPedido() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        Tarjeta tramos = new Tarjeta("Descuento por volumen", Tema.C_AMORTIZACION);
        JLabel nota = Tema.etiqueta("Se aplica a cada pieza según sus propias unidades.", Font.PLAIN, 12f,
                Tema.TEXTO_SUAVE);
        nota.setBorder(new EmptyBorder(0, 0, 8, 0));
        JPanel cuerpoTramos = new JPanel(new BorderLayout());
        cuerpoTramos.setOpaque(false);
        cuerpoTramos.add(nota, BorderLayout.NORTH);
        cuerpoTramos.add(panelTramos, BorderLayout.CENTER);
        tramos.add(cuerpoTramos, BorderLayout.CENTER);
        anadirTarjeta(p, 0, 0, tramos);

        Tarjeta gastos = new Tarjeta("Gastos del pedido", Tema.C_ELECTRICIDAD);
        gastos.add(panelGastos, BorderLayout.CENTER);
        anadirTarjeta(p, 0, 1, gastos);

        lineasResumen.setOpaque(false);
        Tarjeta resumen = new Tarjeta("Resumen del presupuesto", Tema.ACENTO_2);
        resumen.add(new javax.swing.JScrollPane(lineasResumen) {
            private static final long serialVersionUID = 1L;
            {
                setBorder(javax.swing.BorderFactory.createEmptyBorder());
                setOpaque(false);
                getViewport().setOpaque(false);
                setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
                getVerticalScrollBar().setUnitIncrement(16);
            }
        }, BorderLayout.CENTER);
        anadirTarjeta(p, 1, 0, resumen, 1, 2);

        igualarColumnas(p);
        return p;
    }

    // ================================================================ Piezas

    private void seleccionarPieza(int indice) {
        seleccionada = indice;
        mostrarPieza();
        recalcular();
    }

    private void anadirPieza() {
        Pieza nueva = Pieza.nueva();
        Double precio = preciosMaterial.get(nueva.material());
        if (precio != null) {
            nueva = new Pieza("", nueva.material(), precio, nueva.pesoGramos(), nueva.horasImpresion(),
                    nueva.horasManoObra(), nueva.gastos(), 1, null);
        }
        piezas.add(nueva);
        seleccionarPieza(piezas.size() - 1);
        txtNombre.requestFocusInWindow();
    }

    private void duplicarPieza() {
        Pieza p = piezas.get(seleccionada);
        String nombre = p.nombreVisible(seleccionada) + " (copia)";
        piezas.add(seleccionada + 1, new Pieza(nombre, p.material(), p.precioFilamentoKg(), p.pesoGramos(),
                p.horasImpresion(), p.horasManoObra(), p.gastos(), p.cantidad(), p.imagen()));
        seleccionarPieza(seleccionada + 1);
    }

    private void eliminarPieza() {
        if (piezas.size() <= 1) {
            return;
        }
        Pieza p = piezas.get(seleccionada);
        if (JOptionPane.showConfirmDialog(this, "¿Quitar \"" + p.nombreVisible(seleccionada)
                + "\" del presupuesto?", "Eliminar pieza", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        piezas.remove(seleccionada);
        seleccionarPieza(Math.min(seleccionada, piezas.size() - 1));
    }

    /** Rellena los campos con la pieza seleccionada. */
    private void mostrarPieza() {
        Pieza p = piezas.get(seleccionada);
        cargando = true;
        try {
            txtNombre.setText(p.nombre());
            spUnidades.setValue((double) p.cantidad());
            cbMaterial.setSelectedItem(p.material());
            spPrecioKg.setValue(p.precioFilamentoKg());
            spPeso.setValue(p.pesoGramos());
            long minutosImpresion = Math.round(p.horasImpresion() * 60);
            spHoras.setValue((double) (minutosImpresion / 60));
            spMinutos.setValue((double) (minutosImpresion % 60));
            spMinPreparacion.setValue(Math.round(p.horasManoObra() * 60 * 100) / 100.0);
            panelGastosPieza.setGastos(p.gastos());
        } finally {
            cargando = false;
        }
        mostrarImagen();
        selector.mostrar(piezas, seleccionada);
    }

    private void mostrarImagen() {
        Pieza p = piezas.get(seleccionada);
        imagenPieza.setImagen(p.tieneImagen() ? imagenes.cargar(p.imagen()) : null);
        btnQuitarImagen.setEnabled(p.tieneImagen());
    }

    /** Se llama cuando cambia cualquier campo de la pieza: la actualiza y recalcula. */
    private void piezaEditada() {
        if (cargando) {
            return;
        }
        Pieza actual = piezas.get(seleccionada);
        try {
            piezas.set(seleccionada, new Pieza(txtNombre.getText(), (Material) cbMaterial.getSelectedItem(),
                    num(spPrecioKg), num(spPeso), num(spHoras) + num(spMinutos) / 60.0,
                    num(spMinPreparacion) / 60.0, panelGastosPieza.getGastos(),
                    (int) Math.round(num(spUnidades)), actual.imagen()));
        } catch (IllegalArgumentException ex) {
            lblError.setText(ex.getMessage());
            return;
        }
        selector.mostrar(piezas, seleccionada);
        recalcular();
    }

    private boolean ponerImagen(File archivo) {
        try {
            String nombre = imagenes.guardar(archivo);
            piezas.set(seleccionada, piezas.get(seleccionada).conImagen(nombre));
            mostrarImagen();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Imagen no válida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    // ================================================================ Datos

    /** Lee los datos comunes. Lanza IllegalArgumentException si no son válidos. */
    private Taller tallerActual() {
        return new Taller(num(spPotencia), num(spPrecioKwh), num(spPrecioImpresora), num(spVidaUtil),
                num(spMantenimiento), num(spPrecioHora), num(spFallos), num(spMargen), num(spIva));
    }

    private Pedido pedidoActual() {
        return new Pedido(piezas, panelGastos.getGastos(), panelTramos.getTramos());
    }

    /** Carga un presupuesto (guardado o de la configuración) en la ventana. */
    private void aplicar(Taller t, Pedido p) {
        spPotencia.setValue(t.potenciaW());
        spPrecioKwh.setValue(t.precioKwh());
        spPrecioImpresora.setValue(t.precioImpresora());
        spVidaUtil.setValue(t.vidaUtilHoras());
        spMantenimiento.setValue(t.mantenimientoHora());
        spPrecioHora.setValue(t.precioHoraManoObra());
        spFallos.setValue(t.porcentajeFallos());
        spMargen.setValue(t.margenBeneficio());
        spIva.setValue(t.iva());

        piezas.clear();
        piezas.addAll(p.piezas());
        seleccionada = 0;
        panelGastos.setGastos(p.gastos());
        // Los presupuestos antiguos no tenían descuentos: se mantienen los actuales.
        if (!p.tramos().isEmpty()) {
            panelTramos.setTramos(p.tramos());
        }
        mostrarPieza();
        recalcular();
    }

    private void cargarDatosGuardados() {
        try {
            almacen.cargarConfiguracion().ifPresent(c -> {
                preciosMaterial.putAll(c.preciosMaterial());
                emisor = c.emisor();
                aplicar(c.taller(), c.pedido());
            });
        } catch (IOException ex) {
            mostrarError("No se han podido cargar los últimos valores usados.", ex);
        }
        try {
            historial = almacen.cargarHistorial();
        } catch (IOException ex) {
            mostrarError("No se ha podido cargar el historial de presupuestos.", ex);
        }
        try {
            facturas = almacen.cargarFacturas();
        } catch (IOException ex) {
            mostrarError("No se han podido cargar las facturas.", ex);
        }
        actualizarBotonHistorial();
        actualizarBotonFacturas();
        limpiarImagenes();
    }

    /** Borra las imágenes que ya no usa ni el historial, ni las facturas, ni el presupuesto actual. */
    private void limpiarImagenes() {
        List<Pedido> pedidos = new ArrayList<>();
        pedidos.add(new Pedido(piezas, List.of(), List.of()));
        historial.forEach(h -> pedidos.add(h.pedido()));
        facturas.forEach(f -> pedidos.add(f.pedido()));
        Set<String> usadas = new HashSet<>(Almacen.imagenesDe(pedidos));
        almacen.limpiarImagenes(usadas);
    }

    private void guardarConfiguracion() {
        try {
            almacen.guardarConfiguracion(new Almacen.Configuracion(tallerActual(), pedidoActual(), preciosMaterial,
                    emisor));
        } catch (IllegalArgumentException ex) {
            // Datos no válidos: se conservan los últimos valores guardados.
        } catch (IOException ex) {
            mostrarError("No se han podido guardar los valores actuales.", ex);
        }
    }

    private void guardarPresupuesto() {
        Taller taller;
        Pedido pedido;
        try {
            taller = tallerActual();
            pedido = pedidoActual();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos no válidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = new DialogoGuardar(this, "Guardar presupuesto", ultimoNombre,
                piezas.size() == 1 ? null : "Se guardarán las " + piezas.size() + " piezas.").mostrar();
        if (nombre == null) {
            return;
        }
        ultimoNombre = nombre;
        historial.add(0, new Presupuesto(nombre, LocalDateTime.now().withSecond(0).withNano(0), taller, pedido,
                CalculadoraPrecio.calcularPedido(taller, pedido).total()));
        guardarHistorial();
        guardarConfiguracion();
        actualizarBotonHistorial();
    }

    /** Exporta el presupuesto actual como PDF o como CSV. */
    private void exportarPresupuesto() {
        Taller taller;
        Pedido pedido;
        try {
            taller = tallerActual();
            pedido = pedidoActual();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos no válidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DialogoExportar.Resultado r = new DialogoExportar(this, ultimoNombre).mostrar();
        if (r == null) {
            return;
        }
        exportarDocumento(DocumentoPedido.presupuesto(LocalDate.now(), emisor, r.cliente(), pedido,
                CalculadoraPrecio.calcularPedido(taller, pedido)), r.formato());
    }

    /** Pide dónde guardar el presupuesto o la factura, lo exporta y ofrece abrirlo. */
    private void exportarDocumento(DocumentoPedido doc, DialogoExportar.Formato formato) {
        String tipo = doc.tipo().toString().toLowerCase(Locale.ROOT);
        JFileChooser selectorArchivo = new JFileChooser(carpetaExportacion);
        selectorArchivo.setDialogTitle("Exportar " + tipo);
        selectorArchivo.setAcceptAllFileFilterUsed(false);
        selectorArchivo.setFileFilter(new FileNameExtensionFilter(formato.descripcion + " (*."
                + formato.extension + ")", formato.extension));
        String cliente = doc.cliente().nombre();
        String base = doc.titulo() + (cliente.isEmpty() ? "" : " " + cliente)
                + (doc.esFactura() ? "" : " " + doc.fecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        selectorArchivo.setSelectedFile(new File(base.replaceAll("[\\\\/:*?\"<>|]", "_") + "." + formato.extension));
        if (selectorArchivo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        carpetaExportacion = selectorArchivo.getCurrentDirectory();
        File archivo = selectorArchivo.getSelectedFile();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith("." + formato.extension)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + formato.extension);
        }
        if (archivo.exists() && JOptionPane.showConfirmDialog(this,
                "\"" + archivo.getName() + "\" ya existe. ¿Quieres reemplazarlo?", "Exportar " + tipo,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (formato == DialogoExportar.Formato.PDF) {
                ExportadorPdf.exportar(doc, archivo.toPath(), imagenes::cargar);
            } else {
                ExportadorCsv.exportar(doc, archivo.toPath());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se ha podido exportar.\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int abrir = JOptionPane.showConfirmDialog(this,
                doc.titulo() + " exportado en:\n" + archivo.getAbsolutePath() + "\n\n¿Quieres abrirlo ahora?",
                "Exportar " + tipo, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (abrir == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(archivo);
            } catch (IOException | UnsupportedOperationException ex) {
                JOptionPane.showMessageDialog(this, "No se ha podido abrir el archivo.\n" + ex.getMessage(),
                        "Exportar " + tipo, JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ================================================================ Facturas

    /** Abre "Mis datos fiscales". Devuelve {@code true} si se han guardado cambios. */
    private boolean editarEmisor() {
        Emisor nuevo = new DialogoEmisor(this, emisor).mostrar();
        if (nuevo == null) {
            return false;
        }
        emisor = nuevo;
        guardarConfiguracion();
        return true;
    }

    /** Emite una factura con las piezas e importes de un presupuesto. */
    private void facturar(Taller taller, Pedido pedido, String origen) {
        if (!emisor.completoParaFacturar()) {
            JOptionPane.showMessageDialog(this, "Antes de emitir tu primera factura, completa tus datos fiscales:\n"
                    + "nombre o razón social, NIF y dirección.", "Emitir factura", JOptionPane.INFORMATION_MESSAGE);
            if (!editarEmisor() || !emisor.completoParaFacturar()) {
                return;
            }
        }
        LocalDate hoy = LocalDate.now();
        String numero = Factura.siguienteNumero(facturas, hoy);
        double total = CalculadoraPrecio.calcularPedido(taller, pedido).total();
        Cliente cliente = new DialogoNuevaFactura(this, numero, hoy, emisor, origen, total).mostrar();
        if (cliente == null) {
            return;
        }
        Factura f = new Factura(numero, hoy, emisor, cliente, taller, pedido, total, origen);
        facturas.add(f);
        guardarFacturas();
        actualizarBotonFacturas();
        if (JOptionPane.showConfirmDialog(this, "Factura nº " + numero + " emitida.\n¿Quieres exportarla ahora en PDF?",
                "Factura emitida", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION) {
            exportarDocumento(DocumentoPedido.factura(f), DialogoExportar.Formato.PDF);
        }
    }

    /** Factura lo que hay ahora en la calculadora. */
    private void facturarActual() {
        try {
            facturar(tallerActual(), pedidoActual(), ultimoNombre);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos no válidos", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void abrirFacturas() {
        new DialogoFacturas(this, facturas,
                (f, formato) -> exportarDocumento(DocumentoPedido.factura(f), formato),
                () -> {
                    guardarFacturas();
                    limpiarImagenes();
                    actualizarBotonFacturas();
                },
                this::facturarActual,
                this::editarEmisor).setVisible(true);
    }

    private void guardarFacturas() {
        try {
            almacen.guardarFacturas(facturas);
        } catch (IOException ex) {
            mostrarError("No se han podido guardar las facturas.", ex);
        }
    }

    private void actualizarBotonFacturas() {
        btnFacturas.setText(facturas.isEmpty() ? "Facturas" : "Facturas (" + facturas.size() + ")");
    }

    private void guardarHistorial() {
        try {
            almacen.guardarHistorial(historial);
        } catch (IOException ex) {
            mostrarError("No se ha podido guardar el historial.", ex);
        }
    }

    private void abrirHistorial() {
        new DialogoHistorial(this, imagenes, historial, p -> {
            ultimoNombre = p.nombre();
            aplicar(p.taller(), p.pedido());
            pestanas.mostrar(0);
        }, () -> {
            guardarHistorial();
            limpiarImagenes();
            actualizarBotonHistorial();
        }, p -> facturar(p.taller(), p.pedido(), p.nombre())).setVisible(true);
    }

    private void actualizarBotonHistorial() {
        btnHistorial.setText(historial.isEmpty() ? "Historial" : "Historial (" + historial.size() + ")");
    }

    private void mostrarError(String mensaje, Exception ex) {
        JOptionPane.showMessageDialog(this, mensaje + "\n" + ex.getMessage()
                + "\n\nCarpeta de datos: " + almacen.getCarpeta(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ================================================================ Cálculo

    private void recalcular() {
        try {
            ResumenPedido r = CalculadoraPrecio.calcularPedido(tallerActual(), pedidoActual());

            // Coste de una unidad de la pieza seleccionada
            DesglosePrecio d = r.lineas().get(seleccionada).desglose();
            double[] importes = { d.material(), d.electricidad(), d.amortizacion(), d.manoObra(), d.fallos(),
                    d.gastosPieza() };
            double coste = d.costeTotal() + d.gastosPieza();
            for (int i = 0; i < importes.length; i++) {
                double pct = coste > 0 ? importes[i] / coste * 100 : 0;
                lblImportes[i].setText(EUROS.format(importes[i]));
                lblPorcentajes[i].setText(String.format("%.0f %%", pct));
            }
            grafico.setDatos(importes, EUROS.format(coste), "coste por unidad");
            lblTituloPieza.setText(piezas.get(seleccionada).nombreVisible(seleccionada) + " · venta "
                    + EUROS.format(d.precioSinIva()) + " sin IVA");

            // Total
            int n = piezas.size();
            lblTotal.setText(EUROS.format(r.total()));
            lblPorUnidad.setText(n + (n == 1 ? " pieza · " : " piezas · ") + r.unidades()
                    + (r.unidades() == 1 ? " unidad" : " unidades"));
            lblSinIva.setText(EUROS.format(r.baseImponible()));
            lblDescuento.setText(r.descuento() > 0 ? "−" + EUROS.format(r.descuento()) : "—");
            lblIva.setText(EUROS.format(r.iva()));

            mostrarResumen(r);
            Set<TramoDescuento> activos = new HashSet<>();
            r.lineas().forEach(l -> {
                if (l.tramo() != null) {
                    activos.add(l.tramo());
                }
            });
            panelTramos.setTramosActivos(activos);
            pestanas.setTitulo(0, n == 1 ? "Piezas" : "Piezas (" + n + ")");
            pestanas.setTitulo(2, "Pedido · " + r.unidades() + " uds");
            lblError.setText(" ");
        } catch (IllegalArgumentException ex) {
            lblError.setText(ex.getMessage());
        }
    }

    /** Vuelve a construir las líneas del resumen: una por pieza, más gastos, IVA y total. */
    private void mostrarResumen(ResumenPedido r) {
        lineasResumen.removeAll();
        int fila = 0;
        for (int i = 0; i < r.lineas().size(); i++) {
            ResumenPedido.LineaPieza l = r.lineas().get(i);
            fila = lineaResumen(lineasResumen, fila, etiquetaResumen(l.pieza().nombreVisible(i), true),
                    valorResumen(EUROS.format(l.subtotal()), true));
            fila = lineaResumen(lineasResumen, fila,
                    etiquetaResumen("   " + l.pieza().cantidad() + " × " + EUROS.format(l.precioUnitario()), false),
                    valorResumen("", false));
            if (l.descuento() > 0) {
                JLabel dto = etiquetaResumen("   Descuento " + DocumentoPedido.formatoPorcentaje(l.porcentajeDescuento())
                        + " (desde " + l.tramo().cantidadMinima() + " uds)", false);
                JLabel valor = valorResumen("−" + EUROS.format(l.descuento()), false);
                dto.setForeground(Tema.C_AMORTIZACION.darker());
                valor.setForeground(Tema.C_AMORTIZACION.darker());
                fila = lineaResumen(lineasResumen, fila, dto, valor);
            }
        }
        fila = separadorResumen(lineasResumen, fila);
        fila = lineaResumen(lineasResumen, fila, etiquetaResumen("Gastos del pedido", false),
                valorResumen(r.gastosAdicionales() > 0 ? "+" + EUROS.format(r.gastosAdicionales()) : "—", false));
        fila = lineaResumen(lineasResumen, fila, etiquetaResumen("Base imponible", true),
                valorResumen(EUROS.format(r.baseImponible()), true));
        fila = lineaResumen(lineasResumen, fila,
                etiquetaResumen("IVA (" + DocumentoPedido.formatoPorcentaje(r.porcentajeIva()) + ")", false),
                valorResumen(EUROS.format(r.iva()), false));
        fila = separadorResumen(lineasResumen, fila);
        JLabel total = etiquetaResumen("Total del presupuesto", true);
        total.setFont(Tema.fuente(Font.BOLD, 15f));
        JLabel valorTotal = valorResumen(EUROS.format(r.total()), true);
        valorTotal.setFont(Tema.fuente(Font.BOLD, 18f));
        valorTotal.setForeground(Tema.ACENTO);
        fila = lineaResumen(lineasResumen, fila, total, valorTotal);
        fila = lineaResumen(lineasResumen, fila, etiquetaResumen("Precio medio por unidad", false),
                valorResumen(EUROS.format(r.precioMedioPorUnidad()), false));

        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = fila;
        relleno.weighty = 1;
        lineasResumen.add(new JLabel(), relleno);
        lineasResumen.revalidate();
        lineasResumen.repaint();
    }

    // ================================================================ Construcción de la interfaz

    private JComponent cabecera() {
        JLabel logo = new JLabel(new Tema.IconoLogo(52));

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(Tema.etiqueta("Calculadora de impresión 3D", Font.BOLD, 24f, Tema.TEXTO));
        textos.add(Tema.etiqueta("Presupuestos de una o varias piezas al instante",
                Font.PLAIN, 14f, Tema.TEXTO_SUAVE));

        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 6, 0, 0));
        p.add(logo, BorderLayout.WEST);
        p.add(textos, BorderLayout.CENTER);

        JButton btnGuardar = Tema.boton("Guardar presupuesto", true);
        btnGuardar.addActionListener(e -> guardarPresupuesto());
        btnHistorial.addActionListener(e -> abrirHistorial());
        JButton btnExportar = Tema.boton("Exportar", false);
        btnExportar.setToolTipText("Guarda el presupuesto en PDF o como CSV para Excel");
        btnExportar.addActionListener(e -> exportarPresupuesto());
        JButton btnApoyar = Tema.boton("Apoyar", false);
        btnApoyar.setIcon(new DialogoDonaciones.IconoCorazon(14, Tema.C_GASTOS_PIEZA));
        btnApoyar.setIconTextGap(7);
        btnApoyar.setToolTipText("Apoya el proyecto con una donación");
        btnApoyar.addActionListener(e -> new DialogoDonaciones(this).setVisible(true));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        btnFacturas.setToolTipText("Facturas emitidas y tus datos fiscales");
        btnFacturas.addActionListener(e -> abrirFacturas());
        botones.add(btnApoyar);
        botones.add(btnHistorial);
        botones.add(btnFacturas);
        botones.add(btnExportar);
        botones.add(btnGuardar);
        // GridBagLayout centra los botones verticalmente respecto a la cabecera.
        JPanel centrado = new JPanel(new GridBagLayout());
        centrado.setOpaque(false);
        centrado.add(botones);
        p.add(centrado, BorderLayout.EAST);
        return p;
    }

    private JComponent tarjetaPrecioFinal() {
        Tarjeta t = new Tarjeta(Tema.ACENTO, Tema.ACENTO_2);
        Color blancoSuave = new Color(255, 255, 255, 200);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.add(Tema.etiqueta("TOTAL DEL PRESUPUESTO · IVA INCLUIDO", Font.BOLD, 12f, blancoSuave));
        cuerpo.add(lblTotal);
        cuerpo.add(lblPorUnidad);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 70));
        sep.setBackground(new Color(0, 0, 0, 0));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        cuerpo.add(javax.swing.Box.createVerticalStrut(8));
        cuerpo.add(sep);
        cuerpo.add(javax.swing.Box.createVerticalStrut(8));

        JPanel cifras = new JPanel(new GridLayout(1, 3, 10, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Sin IVA", lblSinIva, blancoSuave));
        cifras.add(cifra("Descuento", lblDescuento, blancoSuave));
        cifras.add(cifra("IVA", lblIva, blancoSuave));
        cuerpo.add(cifras);

        for (java.awt.Component c : cuerpo.getComponents()) {
            ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        }
        t.add(cuerpo, BorderLayout.CENTER);
        return t;
    }

    private static JComponent cifra(String titulo, JLabel valor, Color colorTitulo) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);
        p.add(Tema.etiqueta(titulo, Font.PLAIN, 12f, colorTitulo));
        p.add(valor);
        return p;
    }

    private JComponent tarjetaDesglose() {
        Tarjeta t = new Tarjeta("Coste de una unidad", Tema.ACENTO);

        JPanel graficoCentrado = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        graficoCentrado.setOpaque(false);
        graficoCentrado.add(grafico);

        JPanel leyenda = new JPanel(new GridBagLayout());
        leyenda.setOpaque(false);
        for (int i = 0; i < CONCEPTOS.length; i++) {
            JLabel nombre = Tema.etiqueta(CONCEPTOS[i], Font.PLAIN, 13f, Tema.TEXTO);
            nombre.setIcon(new Tema.IconoPunto(COLORES[i], 18));
            nombre.setIconTextGap(8);
            lblPorcentajes[i] = Tema.etiqueta("", Font.PLAIN, 12f, Tema.TEXTO_SUAVE);
            lblPorcentajes[i].setHorizontalAlignment(SwingConstants.RIGHT);
            lblImportes[i] = Tema.etiqueta("", Font.BOLD, 13f, Tema.TEXTO);
            lblImportes[i].setHorizontalAlignment(SwingConstants.RIGHT);

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = i;
            c.insets = new Insets(4, 0, 4, 0);
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            leyenda.add(nombre, c);
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(4, 8, 4, 12);
            leyenda.add(lblPorcentajes[i], c);
            c.insets = new Insets(4, 0, 4, 0);
            leyenda.add(lblImportes[i], c);
        }

        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.setOpaque(false);
        arriba.add(lblTituloPieza, BorderLayout.NORTH);
        arriba.add(graficoCentrado, BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(leyenda, BorderLayout.CENTER);
        cuerpo.add(lblError, BorderLayout.SOUTH);
        t.add(cuerpo, BorderLayout.CENTER);
        return t;
    }

    private static void anadirTarjeta(JPanel panel, int columna, int fila, JComponent tarjeta) {
        anadirTarjeta(panel, columna, fila, tarjeta, 1, 1);
    }

    /** Coloca una tarjeta en la cuadrícula de dos columnas, ocupando {@code alto} filas. */
    private static void anadirTarjeta(JPanel panel, int columna, int fila, JComponent tarjeta,
            double pesoAlto, int alto) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = columna;
        c.gridy = fila;
        c.gridheight = alto;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = pesoAlto;
        c.insets = new Insets(0, columna == 0 ? 0 : 3, 6, columna == 0 ? 3 : 0);
        panel.add(tarjeta, c);
    }

    /**
     * Las dos columnas con el mismo ancho mínimo (el de la tarjeta más ancha
     * que ocupa una sola columna).
     */
    private static void igualarColumnas(JPanel panel) {
        GridBagLayout layout = (GridBagLayout) panel.getLayout();
        int ancho = 0;
        for (java.awt.Component tarjeta : panel.getComponents()) {
            if (layout.getConstraints(tarjeta).gridwidth == 1 && tarjeta instanceof Tarjeta) {
                ancho = Math.max(ancho, tarjeta.getPreferredSize().width);
            }
        }
        layout.columnWidths = new int[] { ancho + 3, ancho + 3 };
    }

    /** Crea una tarjeta con título y filas (etiqueta, componente, unidad). */
    private static JComponent seccion(String titulo, Color color, Object... filas) {
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        for (int i = 0; i < filas.length; i += 3) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = i / 3;
            c.insets = new Insets(5, 0, 5, 10);
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            campos.add(Tema.etiqueta((String) filas[i], Font.PLAIN, 13f, Tema.TEXTO_SUAVE), c);

            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 0, 5, 6);
            campos.add((JComponent) filas[i + 1], c);

            JLabel unidad = Tema.etiqueta((String) filas[i + 2], Font.PLAIN, 12f, Tema.TEXTO_SUAVE);
            unidad.setPreferredSize(new Dimension(40, unidad.getPreferredSize().height));
            c.insets = new Insets(5, 0, 5, 0);
            campos.add(unidad, c);
        }
        // Empuja las filas hacia arriba si la tarjeta es más alta que su contenido.
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = filas.length / 3;
        relleno.weighty = 1;
        campos.add(new JLabel(), relleno);

        Tarjeta t = new Tarjeta(titulo, color);
        t.add(campos, BorderLayout.CENTER);
        return t;
    }

    private static JLabel etiquetaResumen(String texto, boolean negrita) {
        return Tema.etiqueta(texto, negrita ? Font.BOLD : Font.PLAIN, 13f,
                negrita ? Tema.TEXTO : Tema.TEXTO_SUAVE);
    }

    private static JLabel valorResumen(String texto, boolean negrita) {
        JLabel l = Tema.etiqueta(texto, negrita ? Font.BOLD : Font.PLAIN, 13f, Tema.TEXTO);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private static int lineaResumen(JPanel p, int fila, JLabel etiqueta, JLabel valor) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(3, 0, 3, 12);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        p.add(etiqueta, c);
        c.weightx = 0;
        c.insets = new Insets(3, 0, 3, 4);
        c.anchor = GridBagConstraints.EAST;
        p.add(valor, c);
        return fila + 1;
    }

    private static int separadorResumen(JPanel p, int fila) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        JSeparator s = new JSeparator();
        s.setForeground(Tema.BORDE);
        p.add(s, c);
        return fila + 1;
    }

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }
}
