package com.impresion3d.modelo;

import java.util.Collections;
import java.util.List;

/**
 * Una pieza del presupuesto, con sus propios datos de impresión, sus gastos,
 * sus unidades y, opcionalmente, una imagen.
 *
 * @param nombre            nombre opcional (cadena vacía si no tiene)
 * @param precioFilamentoKg precio del material en €/kg
 * @param pesoGramos        peso de una unidad (incluidos soportes) en gramos
 * @param horasImpresion    tiempo de impresión de una unidad en horas
 * @param horasManoObra     tiempo de preparación y postprocesado de una unidad en horas
 * @param gastos            gastos adicionales de cada unidad (imanes, pintura...), sin margen
 * @param cantidad          unidades de esta pieza
 * @param imagen            nombre del archivo de imagen en el almacén, o {@code null}
 */
public record Pieza(
        String nombre,
        Material material,
        double precioFilamentoKg,
        double pesoGramos,
        double horasImpresion,
        double horasManoObra,
        List<GastoAdicional> gastos,
        int cantidad,
        String imagen) {

    public Pieza {
        nombre = Textos.limpiar(nombre);
        if (material == null) {
            material = Material.OTRO;
        }
        if (precioFilamentoKg < 0 || pesoGramos < 0 || horasImpresion < 0 || horasManoObra < 0) {
            throw new IllegalArgumentException("Ningún valor de la pieza puede ser negativo.");
        }
        if (cantidad < 1) {
            throw new IllegalArgumentException("Cada pieza debe tener al menos 1 unidad.");
        }
        gastos = Textos.copiaInmutable(gastos);
        if (Textos.vacio(imagen)) {
            imagen = null;
        }
    }

    /** Pieza nueva con valores habituales. */
    public static Pieza nueva() {
        return new Pieza("", Material.PLA, Material.PLA.getPrecioKg(), 50, 3, 0.25,
                Collections.emptyList(), 1, null);
    }

    /** Combina los datos de la pieza con los comunes para calcular el precio de una unidad. */
    public DatosImpresion datos(Taller t) {
        return new DatosImpresion(pesoGramos, precioFilamentoKg, horasImpresion, t.potenciaW(), t.precioKwh(),
                t.precioImpresora(), t.vidaUtilHoras(), t.mantenimientoHora(), horasManoObra,
                t.precioHoraManoObra(), t.porcentajeFallos(), t.margenBeneficio(), t.iva());
    }

    public boolean tieneImagen() {
        return imagen != null;
    }

    /** Nombre para mostrar: el suyo o "Pieza N" según su posición (empezando en 0). */
    public String nombreVisible(int indice) {
        return nombre.isEmpty() ? "Pieza " + (indice + 1) : nombre;
    }

    /** Peso, tiempo de impresión y material, para listados: "PLA · 50 g · 3 h 0 min". */
    public String resumenTecnico() {
        long minutos = Math.round(horasImpresion * 60);
        return material + " · " + String.format("%.0f g", pesoGramos) + " · " + (minutos / 60) + " h "
                + (minutos % 60) + " min";
    }

    public Pieza conImagen(String nuevaImagen) {
        return new Pieza(nombre, material, precioFilamentoKg, pesoGramos, horasImpresion, horasManoObra, gastos,
                cantidad, nuevaImagen);
    }
}
