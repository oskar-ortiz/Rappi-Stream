package com.rappi.stream.application;

import com.rappi.stream.domain.EstadoPedido;

import java.util.List;
import java.util.UUID;

public interface AuditoriaEventoPort {

    void registrarEvento(UUID pedidoId, String tipoEvento,
                         EstadoPedido estadoAnterior, EstadoPedido estadoNuevo,
                         UUID repartidorId, String payloadJson);

    List<AuditoriaEvento> historialPorPedido(UUID pedidoId);

    // Registro simple de auditoría
    record AuditoriaEvento(
            UUID id,
            UUID pedidoId,
            String tipoEvento,
            String estadoAnterior,
            String estadoNuevo,
            UUID repartidorId,
            String fechaEvento
    ) {}
}