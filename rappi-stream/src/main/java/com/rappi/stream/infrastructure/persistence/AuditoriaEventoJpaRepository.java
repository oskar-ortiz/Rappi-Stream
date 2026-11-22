package com.rappi.stream.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaEventoJpaRepository extends JpaRepository<AuditoriaEventoJpaEntity, UUID> {

    List<AuditoriaEventoJpaEntity> findByPedidoIdOrderByFechaEventoAsc(UUID pedidoId);

    List<AuditoriaEventoJpaEntity> findTop50ByOrderByFechaEventoDesc();
}