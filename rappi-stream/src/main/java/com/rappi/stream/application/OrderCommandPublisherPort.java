package com.rappi.stream.application;

import com.rappi.stream.domain.OrderCreatedCommand;
import java.util.UUID;

public interface OrderCommandPublisherPort {
    void publicarNuevoPedido(OrderCreatedCommand command, UUID orderId);
}