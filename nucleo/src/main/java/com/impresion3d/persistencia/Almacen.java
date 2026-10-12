package com.impresion3d.persistencia;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.impresion3d.modelo.Cliente;
import com.impresion3d.modelo.DatosImpresion;
import com.impresion3d.modelo.Emisor;
import com.impresion3d.modelo.Factura;
import com.impresion3d.modelo.GastoAdicional;
import com.impresion3d.modelo.Material;
import com.impresion3d.modelo.Pedido;
import com.impresion3d.modelo.Pieza;
import com.impresion3d.modelo.Presupuesto;
import com.impresion3d.modelo.Taller;
import com.impresion3d.modelo.Textos;
import com.impresion3d.modelo.TramoDescuento;

/**
 * Guarda en disco la última configuración usada y el historial de
 * presupuestos. Es común a la app de escritorio y a la de Android; cada una
 * indica en qué carpeta trabaja:
 * <ul>
 * <li>{@code configuracion.properties}: últimos valores introducidos.</li>
 * <li>{@code historial.csv}: presupuestos guardados, uno por línea separado por ";".</li>
 * <li>{@code imagenes/}: imágenes de las piezas (las lee y escribe cada app,
 * porque Windows y Android manejan las imágenes de forma distinta).</li>
 * </ul>
 * Lee también los formatos de versiones anteriores (presupuestos de una sola
 * pieza) y los convierte al actual.
 */
public class Almacen {

    // ---- Formato actual del historial: nombre;fecha;taller;piezas;gastos;descuentos;total
    private static final int COLUMNAS = 7;
    private static final String CABECERA = "# v2 nombre;fecha;taller;piezas;gastos;descuentos;total";

    // ---- Formatos antiguos (una pieza por presupuesto)
    /** Claves de cada valor de {@link DatosImpresion}, en el orden de {@code comoArray()}. */
    private static final String[] CAMPOS_ANTIGUOS = { "pesoGramos", "precioFilamentoKg", "horasImpresion",
            "potenciaW", "precioKwh", "precioImpresora", "vidaUtilHoras", "mantenimientoHora",
            "horasManoObra", "precioHoraManoObra", "porcentajeFallos", "margenBeneficio", "iva" };
    private static final int ANTIGUO_CON_IMAGEN = 5 + CAMPOS_ANTIGUOS.length;
    private static final int ANTIGUO_CON_PEDIDO = ANTIGUO_CON_IMAGEN + 4;
    private static final int ANTIGUO_CON_GASTOS_PIEZA = ANTIGUO_CON_PEDIDO + 1;

    /** Claves de la configuración para los datos comunes, en el orden de {@code Taller.comoArray()}. */
    private static final String[] CAMPOS_TALLER = { "taller.potenciaW", "taller.precioKwh",
            "taller.precioImpresora", "taller.vidaUtilHoras", "taller.mantenimientoHora",
            "taller.precioHoraManoObra", "taller.porcentajeFallos", "taller.margenBeneficio", "taller.iva" };

    private static final String SEP = ";";
    /** Separadores dentro de las columnas de piezas, gastos y descuentos. */
    private static final String SEP_ELEMENTOS = "|";
    private static final String SEP_CAMPOS = ",";
    /** Solo se aceptan nombres generados por la aplicación, para no salir de la carpeta. */
    private static final String PATRON_IMAGEN = "[A-Za-z0-9-]+\\.(png|jpg)";

    /** Lado máximo, en píxeles, al que se reducen las imágenes que se guardan. */
    public static final int MAX_LADO_IMAGEN = 1200;

    /**
     * Última configuración usada en la calculadora.
     *
     * @param preciosMaterial último precio por kg usado con cada material
     * @param emisor          tus datos fiscales para presupuestos y facturas
     */
    public record Configuracion(Taller taller, Pedido pedido, Map<Material, Double> preciosMaterial,
            Emisor emisor) {

        public Configuracion {
            preciosMaterial = preciosMaterial.isEmpty() ? new EnumMap<>(Material.class)
                    : new EnumMap<>(preciosMaterial);
            if (emisor == null) {
                emisor = Emisor.VACIO;
            }
        }
    }

    /** Claves de los datos del emisor en la configuración. */
    private static final String[] CAMPOS_EMISOR = { "emisor.nombre", "emisor.nif", "emisor.direccion",
            "emisor.email", "emisor.telefono" };
    /** Columnas de facturas.csv: numero;fecha;emisor;cliente;taller;piezas;gastos;descuentos;total;origen */
    private static final int COLUMNAS_FACTURA = 10;

    private final Path carpeta;
    private final Path carpetaImagenes;
    private final Path ficheroConfig;
    private final Path ficheroHistorial;
    private final Path ficheroFacturas;

    /** Almacén en la carpeta {@code .calculadora3d} del usuario (app de escritorio). */
    public Almacen() {
        this(Paths.get(System.getProperty("user.home"), ".calculadora3d"));
    }

    public Almacen(Path carpeta) {
        this.carpeta = carpeta;
        this.carpetaImagenes = carpeta.resolve("imagenes");
        this.ficheroConfig = carpeta.resolve("configuracion.properties");
        this.ficheroHistorial = carpeta.resolve("historial.csv");
        this.ficheroFacturas = carpeta.resolve("facturas.csv");
    }

    public Path getCarpeta() {
        return carpeta;
    }

    // ================================================================ Configuración

    public Optional<Configuracion> cargarConfiguracion() throws IOException {
        if (!Files.exists(ficheroConfig)) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(ficheroConfig, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        try {
            Configuracion c = p.getProperty("taller.iva") != null ? leerConfiguracion(p) : leerConfiguracionAntigua(p);
            return Optional.of(c);
        } catch (RuntimeException e) {
            throw new IOException("El archivo de configuración está dañado: " + ficheroConfig, e);
        }
    }

    private static Configuracion leerConfiguracion(Properties p) {
        double[] v = new double[CAMPOS_TALLER.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = Double.parseDouble(p.getProperty(CAMPOS_TALLER[i]));
        }
        Pedido pedido;
        try {
            pedido = new Pedido(decodificarPiezas(p.getProperty("pedido.piezas", "")),
                    decodificarGastos(p.getProperty("pedido.gastos", "")),
                    decodificarTramos(p.getProperty("pedido.descuentos", "")));
        } catch (RuntimeException e) {
            pedido = Pedido.nuevo();
        }
        String[] e = new String[CAMPOS_EMISOR.length];
        for (int i = 0; i < e.length; i++) {
            e[i] = p.getProperty(CAMPOS_EMISOR[i], "");
        }
        return new Configuracion(Taller.desdeArray(v), pedido, leerPreciosMaterial(p),
                new Emisor(e[0], e[1], e[2], e[3], e[4]));
    }

    /** Configuración de las versiones de una sola pieza. */
    private static Configuracion leerConfiguracionAntigua(Properties p) {
        double[] v = new double[CAMPOS_ANTIGUOS.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = Double.parseDouble(p.getProperty(CAMPOS_ANTIGUOS[i]));
        }
        DatosImpresion d = DatosImpresion.desdeArray(v);
        Material material = Material.desdeId(p.getProperty("material"));

        int cantidad = 1;
        List<GastoAdicional> gastosPieza = Collections.emptyList();
        List<GastoAdicional> gastos = Collections.emptyList();
        List<TramoDescuento> tramos = Pedido.TRAMOS_POR_DEFECTO;
        if (p.getProperty("pedido.cantidad") != null) {
            try {
                cantidad = Integer.parseInt(p.getProperty("pedido.cantidad"));
                gastosPieza = decodificarGastos(p.getProperty("pieza.gastos", ""));
                gastos = decodificarGastos(p.getProperty("pedido.gastos", ""));
                tramos = decodificarTramos(p.getProperty("pedido.descuentos", ""));
            } catch (RuntimeException e) {
                // Datos del pedido dañados: se usan los valores por defecto.
            }
        }
        Pieza pieza = new Pieza("", material, d.precioFilamentoKg(), d.pesoGramos(), d.horasImpresion(),
                d.horasManoObra(), gastosPieza, Math.max(1, cantidad), null);
        return new Configuracion(Taller.de(d), new Pedido(Collections.singletonList(pieza), gastos, tramos),
                leerPreciosMaterial(p), Emisor.VACIO);
    }

    /** Precios por material ("precio.PLA=18.5"...). Los que falten o no sean válidos se ignoran. */
    private static Map<Material, Double> leerPreciosMaterial(Properties p) {
        Map<Material, Double> precios = new EnumMap<>(Material.class);
        for (Material m : Material.values()) {
            String valor = p.getProperty("precio." + m.name());
            if (valor == null) {
                continue;
            }
            try {
                double precio = Double.parseDouble(valor);
                if (precio >= 0) {
                    precios.put(m, precio);
                }
            } catch (NumberFormatException e) {
                // Valor dañado: se usa el precio orientativo del material.
            }
        }
        return precios;
    }

    public void guardarConfiguracion(Configuracion config) throws IOException {
        Properties p = new Properties();
        double[] v = config.taller().comoArray();
        for (int i = 0; i < CAMPOS_TALLER.length; i++) {
            p.setProperty(CAMPOS_TALLER[i], Double.toString(v[i]));
        }
        p.setProperty("pedido.piezas", codificarPiezas(config.pedido().piezas()));
        p.setProperty("pedido.gastos", codificarGastos(config.pedido().gastos()));
        p.setProperty("pedido.descuentos", codificarTramos(config.pedido().tramos()));
        config.preciosMaterial().forEach((m, precio) ->
                p.setProperty("precio." + m.name(), Double.toString(precio)));
        Emisor e = config.emisor();
        String[] valores = { e.nombre(), e.nif(), e.direccion(), e.email(), e.telefono() };
        for (int i = 0; i < CAMPOS_EMISOR.length; i++) {
            p.setProperty(CAMPOS_EMISOR[i], valores[i]);
        }
        Files.createDirectories(carpeta);
        try (Writer w = Files.newBufferedWriter(ficheroConfig, StandardCharsets.UTF_8)) {
            p.store(w, "Calculadora de impresión 3D - últimos valores usados");
        }
    }

    // ================================================================ Historial

    /** Carga el historial. Las líneas que no se puedan leer se ignoran. */
    public List<Presupuesto> cargarHistorial() throws IOException {
        List<Presupuesto> lista = new ArrayList<>();
        if (!Files.exists(ficheroHistorial)) {
            return lista;
        }
        for (String linea : Files.readAllLines(ficheroHistorial, StandardCharsets.UTF_8)) {
            if (Textos.vacio(linea) || linea.startsWith("#")) {
                continue;
            }
            String[] c = linea.split(SEP, -1);
            try {
                Presupuesto p = c.length == COLUMNAS ? leerPresupuesto(c) : leerPresupuestoAntiguo(c);
                if (p != null) {
                    lista.add(p);
                }
            } catch (RuntimeException e) {
                // Línea dañada: se descarta y se sigue con el resto.
            }
        }
        return lista;
    }

    private static Presupuesto leerPresupuesto(String[] c) {
        String[] t = c[2].split(SEP_CAMPOS, -1);
        double[] v = new double[t.length];
        for (int i = 0; i < t.length; i++) {
            v[i] = Double.parseDouble(t[i]);
        }
        Pedido pedido = new Pedido(decodificarPiezas(c[3]), decodificarGastos(c[4]), decodificarTramos(c[5]));
        return new Presupuesto(decodificar(c[0]), LocalDateTime.parse(c[1]), Taller.desdeArray(v), pedido,
                Double.parseDouble(c[6]));
    }

    /** Líneas de las versiones de una sola pieza; devuelve null si el formato no es conocido. */
    private static Presupuesto leerPresupuestoAntiguo(String[] c) {
        if (c.length != ANTIGUO_CON_GASTOS_PIEZA && c.length != ANTIGUO_CON_PEDIDO
                && c.length != ANTIGUO_CON_IMAGEN && c.length != ANTIGUO_CON_IMAGEN - 1) {
            return null;
        }
        double[] v = new double[CAMPOS_ANTIGUOS.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = Double.parseDouble(c[3 + i]);
        }
        DatosImpresion d = DatosImpresion.desdeArray(v);
        double precio = Double.parseDouble(c[3 + CAMPOS_ANTIGUOS.length]);
        int colImagen = ANTIGUO_CON_IMAGEN - 1;
        String imagen = c.length > colImagen && esNombreImagen(c[colImagen]) ? c[colImagen] : null;

        int cantidad = 1;
        List<GastoAdicional> gastosPieza = Collections.emptyList();
        List<GastoAdicional> gastos = Collections.emptyList();
        List<TramoDescuento> tramos = Collections.emptyList();
        double total = precio;
        if (c.length >= ANTIGUO_CON_PEDIDO) {
            int i = ANTIGUO_CON_IMAGEN;
            cantidad = Integer.parseInt(c[i]);
            gastos = decodificarGastos(c[i + 1]);
            tramos = decodificarTramos(c[i + 2]);
            total = Double.parseDouble(c[i + 3]);
            if (c.length == ANTIGUO_CON_GASTOS_PIEZA) {
                gastosPieza = decodificarGastos(c[i + 4]);
            }
        }
        Pieza pieza = new Pieza("", Material.desdeId(c[2]), d.precioFilamentoKg(), d.pesoGramos(),
                d.horasImpresion(), d.horasManoObra(), gastosPieza, Math.max(1, cantidad), imagen);
        return new Presupuesto(c[0], LocalDateTime.parse(c[1]), Taller.de(d),
                new Pedido(Collections.singletonList(pieza), gastos, tramos), total);
    }

    public void guardarHistorial(List<Presupuesto> lista) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add(CABECERA);
        for (Presupuesto p : lista) {
            lineas.add(String.join(SEP,
                    codificar(p.nombre()),
                    p.fecha().toString(),
                    unirNumeros(p.taller().comoArray()),
                    codificarPiezas(p.pedido().piezas()),
                    codificarGastos(p.pedido().gastos()),
                    codificarTramos(p.pedido().tramos()),
                    Double.toString(p.total())));
        }

        // Se escribe primero en un archivo temporal para no perder el historial si algo falla.
        Files.createDirectories(carpeta);
        Path temporal = ficheroHistorial.resolveSibling("historial.csv.tmp");
        Files.write(temporal, lineas, StandardCharsets.UTF_8);
        Files.move(temporal, ficheroHistorial, StandardCopyOption.REPLACE_EXISTING);
    }

    // ================================================================ Facturas

    /** Carga las facturas emitidas, en el orden en que se guardaron. */
    public List<Factura> cargarFacturas() throws IOException {
        List<Factura> lista = new ArrayList<>();
        if (!Files.exists(ficheroFacturas)) {
            return lista;
        }
        for (String linea : Files.readAllLines(ficheroFacturas, StandardCharsets.UTF_8)) {
            if (Textos.vacio(linea) || linea.startsWith("#")) {
                continue;
            }
            String[] c = linea.split(SEP, -1);
            if (c.length != COLUMNAS_FACTURA) {
                continue;
            }
            try {
                String[] e = camposCodificados(c[2], 5);
                String[] cl = camposCodificados(c[3], 4);
                String[] t = c[4].split(SEP_CAMPOS, -1);
                double[] v = new double[t.length];
                for (int i = 0; i < t.length; i++) {
                    v[i] = Double.parseDouble(t[i]);
                }
                lista.add(new Factura(decodificar(c[0]), LocalDate.parse(c[1]),
                        new Emisor(e[0], e[1], e[2], e[3], e[4]), new Cliente(cl[0], cl[1], cl[2], cl[3]),
                        Taller.desdeArray(v),
                        new Pedido(decodificarPiezas(c[5]), decodificarGastos(c[6]), decodificarTramos(c[7])),
                        Double.parseDouble(c[8]), decodificar(c[9])));
            } catch (RuntimeException ex) {
                // Línea dañada: se descarta y se sigue con el resto.
            }
        }
        return lista;
    }

    public void guardarFacturas(List<Factura> facturas) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("# numero;fecha;emisor;cliente;taller;piezas;gastos;descuentos;total;origen");
        for (Factura f : facturas) {
            Emisor e = f.emisor();
            Cliente cl = f.cliente();
            lineas.add(String.join(SEP,
                    codificar(f.numero()),
                    f.fecha().toString(),
                    unirCodificados(e.nombre(), e.nif(), e.direccion(), e.email(), e.telefono()),
                    unirCodificados(cl.nombre(), cl.nif(), cl.direccion(), cl.email()),
                    unirNumeros(f.taller().comoArray()),
                    codificarPiezas(f.pedido().piezas()),
                    codificarGastos(f.pedido().gastos()),
                    codificarTramos(f.pedido().tramos()),
                    Double.toString(f.total()),
                    codificar(f.presupuestoOrigen())));
        }
        Files.createDirectories(carpeta);
        Path temporal = ficheroFacturas.resolveSibling("facturas.csv.tmp");
        Files.write(temporal, lineas, StandardCharsets.UTF_8);
        Files.move(temporal, ficheroFacturas, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String unirCodificados(String... valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(SEP_CAMPOS);
            }
            sb.append(codificar(valores[i]));
        }
        return sb.toString();
    }

    private static String[] camposCodificados(String texto, int esperados) {
        String[] c = texto.split(SEP_CAMPOS, -1);
        if (c.length != esperados) {
            throw new IllegalArgumentException("Se esperaban " + esperados + " campos.");
        }
        for (int i = 0; i < c.length; i++) {
            c[i] = decodificar(c[i]);
        }
        return c;
    }

    // ================================================================ Imágenes (solo archivos)

    /** {@code true} si el nombre es uno generado por la aplicación. */
    public static boolean esNombreImagen(String nombre) {
        return nombre != null && nombre.matches(PATRON_IMAGEN);
    }

    /**
     * Reserva un nombre nuevo para una imagen y devuelve su ruta (creando la
     * carpeta si hace falta). La app es quien escribe en ella la imagen.
     *
     * @param transparente {@code true} para PNG (con transparencia), {@code false} para JPG
     */
    public Path nuevaRutaImagen(boolean transparente) throws IOException {
        Files.createDirectories(carpetaImagenes);
        return carpetaImagenes.resolve(UUID.randomUUID() + (transparente ? ".png" : ".jpg"));
    }

    /** Ruta de una imagen guardada, o {@code null} si el nombre no es válido o no existe. */
    public Path rutaImagen(String nombre) {
        if (!esNombreImagen(nombre)) {
            return null;
        }
        Path ruta = carpetaImagenes.resolve(nombre);
        return Files.exists(ruta) ? ruta : null;
    }

    /**
     * Borra las imágenes que ya no usa ningún presupuesto ni la configuración
     * actual. Las imágenes no se borran en el momento de quitarlas, porque
     * pueden seguir usándose en otro presupuesto del historial.
     */
    public void limpiarImagenes(Collection<String> usadas) {
        if (!Files.isDirectory(carpetaImagenes)) {
            return;
        }
        Set<String> enUso = new HashSet<>(usadas);
        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(carpetaImagenes)) {
            for (Path a : archivos) {
                String nombre = a.getFileName().toString();
                if (esNombreImagen(nombre) && !enUso.contains(nombre)) {
                    Files.deleteIfExists(a);
                }
            }
        } catch (IOException e) {
            // No es grave: solo quedan archivos sin usar.
        }
    }

    /** Imágenes que usan unas piezas (para {@link #limpiarImagenes}). */
    public static Set<String> imagenesDe(Collection<Pedido> pedidos) {
        Set<String> nombres = new HashSet<>();
        for (Pedido p : pedidos) {
            for (Pieza pieza : p.piezas()) {
                if (pieza.tieneImagen()) {
                    nombres.add(pieza.imagen());
                }
            }
        }
        return nombres;
    }

    // ================================================================ Codificación

    /**
     * Piezas como "nombre,material,€/kg,g,h,hMO,gastos,uds,imagen|...". El
     * nombre y los gastos van codificados para que no rompan el formato.
     */
    private static String codificarPiezas(List<Pieza> piezas) {
        return piezas.stream()
                .map(p -> String.join(SEP_CAMPOS,
                        codificar(p.nombre()),
                        p.material().name(),
                        Double.toString(p.precioFilamentoKg()),
                        Double.toString(p.pesoGramos()),
                        Double.toString(p.horasImpresion()),
                        Double.toString(p.horasManoObra()),
                        codificar(codificarGastos(p.gastos())),
                        Integer.toString(p.cantidad()),
                        p.tieneImagen() ? p.imagen() : ""))
                .collect(Collectors.joining(SEP_ELEMENTOS));
    }

    private static List<Pieza> decodificarPiezas(String texto) {
        List<Pieza> piezas = new ArrayList<>();
        if (Textos.vacio(texto)) {
            return piezas;
        }
        for (String elemento : texto.split(Pattern.quote(SEP_ELEMENTOS))) {
            String[] c = elemento.split(SEP_CAMPOS, -1);
            if (c.length == 9) {
                piezas.add(new Pieza(decodificar(c[0]), Material.desdeId(c[1]), Double.parseDouble(c[2]),
                        Double.parseDouble(c[3]), Double.parseDouble(c[4]), Double.parseDouble(c[5]),
                        decodificarGastos(decodificar(c[6])), Integer.parseInt(c[7]),
                        esNombreImagen(c[8]) ? c[8] : null));
            }
        }
        return piezas;
    }

    /**
     * Gastos como "concepto,importe,U|concepto,importe,P" (U = por unidad,
     * P = por pedido).
     */
    private static String codificarGastos(List<GastoAdicional> gastos) {
        return gastos.stream()
                .map(g -> codificar(g.concepto()) + SEP_CAMPOS + g.importe() + SEP_CAMPOS
                        + (g.porUnidad() ? "U" : "P"))
                .collect(Collectors.joining(SEP_ELEMENTOS));
    }

    private static List<GastoAdicional> decodificarGastos(String texto) {
        List<GastoAdicional> gastos = new ArrayList<>();
        if (Textos.vacio(texto)) {
            return gastos;
        }
        for (String elemento : texto.split(Pattern.quote(SEP_ELEMENTOS))) {
            String[] c = elemento.split(SEP_CAMPOS, -1);
            if (c.length == 3) {
                gastos.add(new GastoAdicional(decodificar(c[0]), Double.parseDouble(c[1]), "U".equals(c[2])));
            }
        }
        return gastos;
    }

    /** Descuentos como "10,5.0|25,10.0" (cantidad mínima, porcentaje). */
    private static String codificarTramos(List<TramoDescuento> tramos) {
        return tramos.stream()
                .map(t -> t.cantidadMinima() + SEP_CAMPOS + t.porcentaje())
                .collect(Collectors.joining(SEP_ELEMENTOS));
    }

    private static List<TramoDescuento> decodificarTramos(String texto) {
        List<TramoDescuento> tramos = new ArrayList<>();
        if (Textos.vacio(texto)) {
            return tramos;
        }
        for (String elemento : texto.split(Pattern.quote(SEP_ELEMENTOS))) {
            String[] c = elemento.split(SEP_CAMPOS, -1);
            if (c.length == 2) {
                tramos.add(new TramoDescuento(Integer.parseInt(c[0]), Double.parseDouble(c[1])));
            }
        }
        return tramos;
    }

    private static String unirNumeros(double[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(SEP_CAMPOS);
            }
            sb.append(v[i]);
        }
        return sb.toString();
    }

    // Se usa la variante con nombre de codificación porque la que recibe un
    // Charset no existe en Android 8.
    private static String codificar(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decodificar(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
