package com.rappi.stream.domain;

public enum EstadoPedido {
    CREADO("Creado"),
    ASIGNADO("Asignado"),
    EN_CAMINO("En camino"),
    ENTREGADO("Entregado");

    private final String descripcion;

    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean puedeTransitarA(EstadoPedido nuevo) {
        return switch (this) {
            case CREADO -> nuevo == ASIGNADO;
            case ASIGNADO -> nuevo == EN_CAMINO;
            case EN_CAMINO -> nuevo == ENTREGADO;
            case ENTREGADO -> false;
        };
    }
}