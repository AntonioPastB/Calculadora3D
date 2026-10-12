package com.impresion3d.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Herramienta de empaquetado: genera un archivo .ico de Windows a partir del
 * logotipo de la aplicación. No forma parte del programa; la usa empaquetar.ps1.
 * <p>
 * Uso: {@code java -cp <clases> com.impresion3d.ui.GeneradorIcono <salida.ico>}
 */
public class GeneradorIcono {

    private static final int[] TAMANOS = { 16, 20, 24, 32, 40, 48, 64, 128, 256 };

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso: GeneradorIcono <salida.ico>");
            System.exit(2);
        }
        List<byte[]> datos = new ArrayList<>();
        for (int t : TAMANOS) {
            BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            new Tema.IconoLogo(t).paintIcon(null, g, 0, 0);
            g.dispose();
            // 256 px se guarda como PNG (lo admite Windows Vista y posteriores);
            // el resto en formato BMP clásico para máxima compatibilidad.
            datos.add(t >= 256 ? png(img) : dib(img));
        }

        int cabecera = 6 + 16 * TAMANOS.length;
        ByteBuffer ico = ByteBuffer.allocate(cabecera + datos.stream().mapToInt(d -> d.length).sum())
                .order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) TAMANOS.length);
        int desplazamiento = cabecera;
        for (int i = 0; i < TAMANOS.length; i++) {
            int t = TAMANOS[i];
            ico.put((byte) (t >= 256 ? 0 : t)).put((byte) (t >= 256 ? 0 : t))
                    .put((byte) 0).put((byte) 0)
                    .putShort((short) 1).putShort((short) 32)
                    .putInt(datos.get(i).length).putInt(desplazamiento);
            desplazamiento += datos.get(i).length;
        }
        datos.forEach(ico::put);
        Files.write(Path.of(args[0]), ico.array());
        System.out.println("Icono generado: " + args[0]);
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** Imagen en formato DIB de 32 bits (BGRA, de abajo arriba) con máscara AND vacía. */
    private static byte[] dib(BufferedImage img) {
        int t = img.getWidth();
        int bytesMascaraFila = ((t + 31) / 32) * 4;
        ByteBuffer b = ByteBuffer.allocate(40 + t * t * 4 + bytesMascaraFila * t)
                .order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(40).putInt(t).putInt(t * 2) // en los .ico la altura incluye la máscara
                .putShort((short) 1).putShort((short) 32)
                .putInt(0).putInt(t * t * 4).putInt(0).putInt(0).putInt(0).putInt(0);
        for (int y = t - 1; y >= 0; y--) {
            for (int x = 0; x < t; x++) {
                int argb = img.getRGB(x, y);
                b.put((byte) argb).put((byte) (argb >> 8)).put((byte) (argb >> 16)).put((byte) (argb >> 24));
            }
        }
        // La máscara AND queda a ceros: la transparencia la da el canal alfa.
        return b.array();
    }
}
