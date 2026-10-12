package com.impresion3d.persistencia;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Generador mínimo de PDF sin librerías externas: cada página es una imagen
 * JPEG que ocupa una hoja A4 completa.
 */
public final class PdfImagenes {

    /** Tamaño A4 en puntos (1/72 de pulgada). */
    public static final double ANCHO_A4 = 595.28;
    public static final double ALTO_A4 = 841.89;

    private PdfImagenes() {
    }

    public static void escribir(List<BufferedImage> paginas, Path destino, String titulo) throws IOException {
        Pdf pdf = new Pdf();
        int numPaginas = paginas.size();
        // Objetos: 1 catálogo, 2 árbol de páginas, 3 información; después,
        // por cada página: página, contenido e imagen.
        int primero = 4;
        pdf.objeto(1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < numPaginas; i++) {
            hijos.append(primero + i * 3).append(" 0 R ");
        }
        pdf.objeto(2, "<< /Type /Pages /Kids [" + hijos + "] /Count " + numPaginas + " >>");
        pdf.objeto(3, "<< /Title " + textoPdf(titulo) + " /Producer (Calculadora de impresion 3D) >>");

        for (int i = 0; i < numPaginas; i++) {
            int pagina = primero + i * 3;
            int contenido = pagina + 1;
            int imagen = pagina + 2;
            BufferedImage img = paginas.get(i);
            byte[] jpeg = jpeg(img);
            String dibujo = String.format(java.util.Locale.ROOT, "q %.2f 0 0 %.2f 0 0 cm /Im0 Do Q",
                    ANCHO_A4, ALTO_A4);

            pdf.objeto(pagina, String.format(java.util.Locale.ROOT,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] "
                            + "/Resources << /XObject << /Im0 %d 0 R >> >> /Contents %d 0 R >>",
                    ANCHO_A4, ALTO_A4, imagen, contenido));
            pdf.flujo(contenido, "", dibujo.getBytes(StandardCharsets.US_ASCII));
            pdf.flujo(imagen, "/Type /XObject /Subtype /Image /Width " + img.getWidth()
                    + " /Height " + img.getHeight()
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode", jpeg);
        }
        Files.write(destino, pdf.terminar(primero + numPaginas * 3, 3));
    }

    private static byte[] jpeg(BufferedImage img) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.92f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** Cadena PDF en UTF-16 (admite acentos y cualquier carácter). */
    private static String textoPdf(String s) {
        StringBuilder sb = new StringBuilder("<FEFF");
        for (char ch : s.toCharArray()) {
            sb.append(String.format("%04X", (int) ch));
        }
        return sb.append('>').toString();
    }

    /** Escribe objetos PDF llevando la cuenta de sus posiciones para la tabla xref. */
    private static final class Pdf {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final List<long[]> posiciones = new ArrayList<>();

        Pdf() {
            texto("%PDF-1.4\n%âãÏÓ\n");
        }

        void objeto(int id, String cuerpo) {
            posiciones.add(new long[] { id, out.size() });
            texto(id + " 0 obj\n" + cuerpo + "\nendobj\n");
        }

        void flujo(int id, String diccionario, byte[] datos) {
            posiciones.add(new long[] { id, out.size() });
            texto(id + " 0 obj\n<< " + diccionario + " /Length " + datos.length + " >>\nstream\n");
            out.writeBytes(datos);
            texto("\nendstream\nendobj\n");
        }

        byte[] terminar(int totalObjetos, int idInfo) {
            long inicioXref = out.size();
            long[] tabla = new long[totalObjetos];
            for (long[] p : posiciones) {
                tabla[(int) p[0]] = p[1];
            }
            StringBuilder sb = new StringBuilder("xref\n0 " + totalObjetos + "\n0000000000 65535 f \n");
            for (int i = 1; i < totalObjetos; i++) {
                sb.append(String.format("%010d 00000 n \n", tabla[i]));
            }
            sb.append("trailer\n<< /Size ").append(totalObjetos).append(" /Root 1 0 R /Info ")
                    .append(idInfo).append(" 0 R >>\nstartxref\n").append(inicioXref).append("\n%%EOF\n");
            texto(sb.toString());
            return out.toByteArray();
        }

        private void texto(String s) {
            out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
        }
    }
}
