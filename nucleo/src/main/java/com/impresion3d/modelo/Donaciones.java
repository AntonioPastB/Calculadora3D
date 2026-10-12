package com.impresion3d.modelo;

/**
 * Datos del apartado "Apoya el proyecto", comunes a la app de escritorio y a
 * la de Android. Para cambiar el enlace de donaciones basta con cambiarlo aquí.
 */
public final class Donaciones {

    /** Enlace de PayPal.me al que llevan los botones de donar. */
    public static final String ENLACE = "https://paypal.me/Incrimus";

    public static final String TITULO = "Apoya el proyecto";

    public static final String TEXTO = "Esta calculadora es gratuita. Si te resulta útil y quieres apoyar "
            + "su desarrollo, puedes hacer una donación voluntaria a través de PayPal. "
            + "Cualquier aportación ayuda a seguir mejorándola. ¡Gracias!";

    private Donaciones() {
    }

    /** {@code false} mientras el enlace sea el de ejemplo ("TU_USUARIO"). */
    public static boolean estaConfigurado() {
        return !ENLACE.contains("TU_USUARIO");
    }
}
