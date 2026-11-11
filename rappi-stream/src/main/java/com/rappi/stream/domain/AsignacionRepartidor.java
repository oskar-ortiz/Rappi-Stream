package com.rappi.stream.domain;

import java.util.UUID;

public class AsignacionRepartidor {
    private UUID id;
    private UUID idPedido;
    private UUID idRepartidor;
    private EstadoPedido estado;
    private String fechaAsignacion;

    public AsignacionRepartidor(UUID idPedido, UUID idRepartidor) {
        this.id = UUID.randomUUID();
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.estado = EstadoPedido.ASIGNADO;
        this.fechaAsignacion = java.time.Instant.now().toString();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdPedido() {
        return idPedido;
    }

    public UUID getIdRepartidor() {
        return idRepartidor;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public String getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void actualizarEstado(EstadoPedido nuevoEstado) {
        if (nuevoEstado == EstadoPedido.ENTREGADO) {
            this.estado = nuevoEstado;
        } else {
            throw new IllegalStateException("Solo se puede actualizar el estado a ENTREGADO");
        }
    }
}