package com.impresion3d.modelo;

/** Materiales habituales con su precio orientativo en €/kg. */
public enum Material {
    PLA("PLA", 20), PETG("PETG", 25), ABS("ABS", 22), ASA("ASA", 28),
    TPU("TPU (flexible)", 35), RESINA("Resina", 40), OTRO("Personalizado", -1);

    private final String nombre;
    private final double precioKg;

    Material(String nombre, double precioKg) {
        this.nombre = nombre;
        this.precioKg = precioKg;
    }

    /** Precio orientativo en €/kg, o -1 si el material es personalizado. */
    public double getPrecioKg() {
        return precioKg;
    }

    /** Devuelve el material con ese identificador, o {@link #OTRO} si no existe. */
    public static Material desdeId(String id) {
        try {
            return valueOf(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            return OTRO;
        }
    }

    @Override
    public String toString() {
        return nombre;
    }
}
