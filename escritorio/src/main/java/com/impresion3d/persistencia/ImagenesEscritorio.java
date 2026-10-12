package com.impresion3d.persistencia;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Lee y guarda las imágenes de los cálculos en la app de escritorio (con
 * {@link ImageIO}). Los archivos van a la carpeta de imágenes del {@link Almacen}.
 */
public class ImagenesEscritorio {

    private final Almacen almacen;

    public ImagenesEscritorio(Almacen almacen) {
        this.almacen = almacen;
    }

    /**
     * Copia una imagen al almacén (reducida si es muy grande) y devuelve el
     * nombre con el que se ha guardado.
     */
    public String guardar(File origen) throws IOException {
        BufferedImage original = ImageIO.read(origen);
        if (original == null) {
            throw new IOException("El archivo no es una imagen compatible (PNG, JPG, GIF o BMP).");
        }
        boolean transparente = original.getColorModel().hasAlpha();
        Path destino = almacen.nuevaRutaImagen(transparente);
        if (!ImageIO.write(reducir(original, transparente), transparente ? "png" : "jpg", destino.toFile())) {
            throw new IOException("No se ha podido guardar la imagen.");
        }
        return destino.getFileName().toString();
    }

    /** Devuelve la imagen guardada con ese nombre, o {@code null} si no existe o no se puede leer. */
    public BufferedImage cargar(String nombre) {
        Path ruta = almacen.rutaImagen(nombre);
        if (ruta == null) {
            return null;
        }
        try {
            return Files.exists(ruta) ? ImageIO.read(ruta.toFile()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage reducir(BufferedImage original, boolean transparente) {
        double escala = Math.min(1.0,
                (double) Almacen.MAX_LADO_IMAGEN / Math.max(original.getWidth(), original.getHeight()));
        int w = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int h = Math.max(1, (int) Math.round(original.getHeight() * escala));
        // Se redibuja siempre para normalizar el tipo de imagen (JPG no admite transparencia).
        BufferedImage destino = new BufferedImage(w, h,
                transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, w, h, null);
        g.dispose();
        return destino;
    }
}
