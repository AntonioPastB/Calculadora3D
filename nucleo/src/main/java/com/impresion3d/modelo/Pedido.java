package com.impresion3d.modelo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lo que se presupuesta: una o varias piezas (cada una con sus unidades), los
 * gastos del pedido y los descuentos por volumen.
 *
 * @param piezas piezas del presupuesto (al menos una)
 * @param gastos gastos del pedido (envío, embalaje...), por pedido o por unidad
 * @param tramos descuentos por volumen; a cada pieza se le aplica el que
 *               corresponde a sus unidades
 */
public record Pedido(List<Pieza> piezas, List<GastoAdicional> gastos, List<TramoDescuento> tramos) {

    /** Tramos que se proponen la primera vez que se usa la aplicación. */
    public static final List<TramoDescuento> TRAMOS_POR_DEFECTO = Textos.copiaInmutable(Arrays.asList(
            new TramoDescuento(10, 5), new TramoDescuento(25, 10), new TramoDescuento(50, 15)));

    public Pedido {
        if (piezas == null || piezas.isEmpty()) {
            throw new IllegalArgumentException("El presupuesto debe tener al menos una pieza.");
        }
        piezas = Textos.copiaInmutable(piezas);
        gastos = Textos.copiaInmutable(gastos);
        tramos = Textos.copiaInmutable(tramos);
    }

    /** Presupuesto nuevo: una pieza, sin gastos y con los descuentos por defecto. */
    public static Pedido nuevo() {
        return new Pedido(Collections.singletonList(Pieza.nueva()), Collections.emptyList(), TRAMOS_POR_DEFECTO);
    }

    /** Unidades totales de todas las piezas. */
    public int unidades() {
        return piezas.stream().mapToInt(Pieza::cantidad).sum();
    }

    /**
     * Tramo de descuento para una cantidad: el de mayor cantidad mínima que no
     * la supere (y, a igualdad, el de mayor porcentaje).
     */
    public Optional<TramoDescuento> tramoPara(int cantidad) {
        return tramos.stream()
                .filter(t -> t.cantidadMinima() <= cantidad)
                .max(Comparator.comparingInt(TramoDescuento::cantidadMinima)
                        .thenComparingDouble(TramoDescuento::porcentaje));
    }

    /** Imagen de la primera pieza que tenga una, para mostrar en listados. */
    public String imagenPrincipal() {
        return piezas.stream().filter(Pieza::tieneImagen).map(Pieza::imagen).findFirst().orElse(null);
    }
}
