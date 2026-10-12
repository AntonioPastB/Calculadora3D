package com.impresion3d.modelo;

/**
 * Datos fiscales de quien emite los presupuestos y las facturas.
 *
 * @param nombre    nombre y apellidos o razón social
 * @param nif       NIF o CIF
 * @param direccion dirección completa (calle, código postal, ciudad...)
 */
public record Emisor(String nombre, String nif, String direccion, String email, String telefono) {

    public static final Emisor VACIO = new Emisor("", "", "", "", "");

    public Emisor {
        nombre = Textos.limpiar(nombre);
        nif = Textos.limpiar(nif);
        direccion = Textos.limpiar(direccion);
        email = Textos.limpiar(email);
        telefono = Textos.limpiar(telefono);
    }

    /** Una factura necesita, como mínimo, nombre, NIF y dirección del emisor. */
    public boolean completoParaFacturar() {
        return !nombre.isEmpty() && !nif.isEmpty() && !direccion.isEmpty();
    }

    public boolean vacio() {
        return nombre.isEmpty() && nif.isEmpty() && direccion.isEmpty() && email.isEmpty() && telefono.isEmpty();
    }
}
