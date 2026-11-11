package com.rappi.stream.application;

import com.rappi.stream.domain.Pedido;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Pedido save(Pedido pedido);

    Optional<Pedido> findById(UUID pedidoId);

    boolean existsById(UUID pedidoId);

    Optional<Pedido> findByIdCliente(UUID idCliente);

    // Métodos para consulta de historial/estadisticas
    int countByEstado(EstadoPedido estado);

    // Método para obtener el último estado known
    Optional<EstadoPedido> ultimoEstado(UUID pedidoId);
}