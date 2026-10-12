package com.impresion3d.modelo;

/**
 * Cliente de un presupuesto o una factura. En los presupuestos basta con el
 * nombre; en las facturas conviene poner también NIF y dirección.
 */
public record Cliente(String nombre, String nif, String direccion, String email) {

    public Cliente {
        nombre = Textos.limpiar(nombre);
        nif = Textos.limpiar(nif);
        direccion = Textos.limpiar(direccion);
        email = Textos.limpiar(email);
    }

    public static Cliente conNombre(String nombre) {
        return new Cliente(nombre, "", "", "");
    }

    public boolean vacio() {
        return nombre.isEmpty() && nif.isEmpty() && direccion.isEmpty() && email.isEmpty();
    }
}
