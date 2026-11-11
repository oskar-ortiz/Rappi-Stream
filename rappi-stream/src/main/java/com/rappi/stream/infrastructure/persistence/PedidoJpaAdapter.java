package com.rappi.stream.infrastructure.persistence;

import com.rappi.stream.application.OrderRepositoryPort;
import com.rappi.stream.domain.Pedido;
import com.rappi.stream.domain.EstadoPedido;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class PedidoJpaAdapter implements OrderRepositoryPort {

    private final PedidoJpaRepository jpaRepository;

    public PedidoJpaAdapter(PedidoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Pedido save(Pedido pedido) {
        PedidoJpaEntity entity = toEntity(pedido);
        PedidoJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Pedido> findById(UUID pedidoId) {
        return jpaRepository.findById(pedidoId).map(this::toDomain);
    }

    @Override
    public boolean existsById(UUID pedidoId) {
        return jpaRepository.existsById(pedidoId);
    }

    @Override
    public Optional<Pedido> findByIdCliente(UUID idCliente) {
        return jpaRepository.findByIdCliente(idCliente).map(this::toDomain);
    }

    @Override
    public int countByEstado(EstadoPedido estado) {
        return (int) jpaRepository.countByEstado(estado);
    }

    @Override
    public Optional<EstadoPedido> ultimoEstado(UUID pedidoId) {
        return jpaRepository.findEstadoById(pedidoId);
    }

    // ===================== Mapeo dominio <-> JPA =====================

    private PedidoJpaEntity toEntity(Pedido pedido) {
        PedidoJpaEntity entity = new PedidoJpaEntity();
        entity.setId(pedido.getId());
        entity.setEstado(pedido.getEstado());
        entity.setIdCliente(pedido.getIdCliente());
        entity.setDireccionEntrega(pedido.getDireccionEntrega());
        entity.setIdRepartidorAsignado(pedido.getIdRepartidorAsignado());
        return entity;
    }

    private Pedido toDomain(PedidoJpaEntity entity) {
        return Pedido.restaurar(
                entity.getId(),
                entity.getEstado(),
                entity.getIdCliente(),
                entity.getDireccionEntrega(),
                entity.getIdRepartidorAsignado()
        );
    }
}