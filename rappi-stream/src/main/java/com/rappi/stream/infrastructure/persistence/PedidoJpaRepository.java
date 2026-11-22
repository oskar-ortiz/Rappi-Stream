package com.rappi.stream.infrastructure.persistence;

import com.rappi.stream.domain.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    Optional<PedidoJpaEntity> findByIdCliente(UUID idCliente);

    long countByEstado(EstadoPedido estado);

    @Query("SELECT p.estado FROM PedidoJpaEntity p WHERE p.id = :pedidoId")
    Optional<EstadoPedido> findEstadoById(@Param("pedidoId") UUID pedidoId);
}