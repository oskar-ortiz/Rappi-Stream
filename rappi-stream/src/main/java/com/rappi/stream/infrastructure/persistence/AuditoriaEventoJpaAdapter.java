package com.rappi.stream.infrastructure.persistence;

import com.rappi.stream.application.AuditoriaEventoPort;
import com.rappi.stream.domain.EstadoPedido;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AuditoriaEventoJpaAdapter implements AuditoriaEventoPort {

    private final AuditoriaEventoJpaRepository jpaRepository;

    public AuditoriaEventoJpaAdapter(AuditoriaEventoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void registrarEvento(UUID pedidoId, String tipoEvento,
                                EstadoPedido estadoAnterior, EstadoPedido estadoNuevo,
                                UUID repartidorId, String payloadJson) {
        AuditoriaEventoJpaEntity entity = new AuditoriaEventoJpaEntity(
                pedidoId,
                tipoEvento,
                estadoAnterior != null ? estadoAnterior.name() : null,
                estadoNuevo.name(),
                repartidorId,
                payloadJson
        );
        jpaRepository.save(entity);
    }

    @Override
    public List<AuditoriaEvento> historialPorPedido(UUID pedidoId) {
        return jpaRepository.findByPedidoIdOrderByFechaEventoAsc(pedidoId)
                .stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private AuditoriaEvento toRecord(AuditoriaEventoJpaEntity entity) {
        return new AuditoriaEvento(
                entity.getId(),
                entity.getPedidoId(),
                entity.getTipoEvento(),
                entity.getEstadoAnterior(),
                entity.getEstadoNuevo(),
                entity.getRepartidorId(),
                entity.getFechaEvento().toString()
        );
    }
}