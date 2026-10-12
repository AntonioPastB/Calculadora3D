package com.impresion3d.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utilidades comunes del núcleo. Solo usan funciones de Java disponibles
 * también en Android 8 (API 26), para que el núcleo sirva en las dos apps.
 */
public final class Textos {

    /** Configuración regional de la aplicación: español de España (coma decimal, €). */
    public static final Locale ES = Locale.forLanguageTag("es-ES");

    private Textos() {
    }

    /** Texto sin espacios al principio ni al final; {@code null} se convierte en "". */
    public static String limpiar(String s) {
        return s == null ? "" : s.trim();
    }

    /** {@code true} si es nulo, vacío o solo tiene espacios. */
    public static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Copia inmutable de una lista. */
    public static <T> List<T> copiaInmutable(List<T> lista) {
        return Collections.unmodifiableList(new ArrayList<>(lista));
    }
}
