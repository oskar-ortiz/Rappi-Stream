package com.rappi.stream.domain;

import java.util.UUID;

public class Pedido {
    private UUID id;
    private EstadoPedido estado;
    private UUID idRepartidorAsignado;
    private UUID idCliente;
    private String direccionEntrega;

    public Pedido(UUID id, UUID idCliente, String direccionEntrega) {
        this.id = id;
        this.idCliente = idCliente;
        this.direccionEntrega = direccionEntrega;
        this.estado = EstadoPedido.CREADO;
        this.idRepartidorAsignado = null;
    }

    /**
     * Fábrica estática para reconstruir el agregado desde persistencia
     * sin validar transiciones (el estado ya fue validado al persistirse).
     * Usada exclusivamente por el adaptador de infraestructura.
     */
    public static Pedido restaurar(UUID id, EstadoPedido estado, UUID idCliente,
                                   String direccionEntrega, UUID idRepartidorAsignado) {
        Pedido pedido = new Pedido(id, idCliente, direccionEntrega);
        pedido.estado = estado;
        pedido.idRepartidorAsignado = idRepartidorAsignado;
        return pedido;
    }

    public UUID getId() {
        return id;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public UUID getIdRepartidorAsignado() {
        return idRepartidorAsignado;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void asignarRepartidor(UUID idRepartidor) {
        if (!EstadoPedido.CREADO.puedeTransitarA(EstadoPedido.ASIGNADO)) {
            throw new IllegalStateException("No se puede asignar repartidor: el pedido no está en estado CREADO");
        }
        this.estado = EstadoPedido.ASIGNADO;
        this.idRepartidorAsignado = idRepartidor;
    }

    public void registrarLlegada() {
        if (!EstadoPedido.ASIGNADO.puedeTransitarA(EstadoPedido.EN_CAMINO)) {
            throw new IllegalStateException("No se puede registrar llegada: el pedido no está en estado ASIGNADO");
        }
        this.estado = EstadoPedido.EN_CAMINO;
    }

    public void completarEntrega() {
        if (!EstadoPedido.EN_CAMINO.puedeTransitarA(EstadoPedido.ENTREGADO)) {
            throw new IllegalStateException("No se puede completar entrega: el pedido no está en estado EN_CAMINO");
        }
        this.estado = EstadoPedido.ENTREGADO;
        this.idRepartidorAsignado = null;
    }

    public boolean isEnCamino() {
        return estado == EstadoPedido.EN_CAMINO;
    }

    public boolean isEntregado() {
        return estado == EstadoPedido.ENTREGADO;
    }

    public boolean isAsignado() {
        return estado == EstadoPedido.ASIGNADO;
    }

    public boolean isCreado() {
        return estado == EstadoPedido.CREADO;
    }
}