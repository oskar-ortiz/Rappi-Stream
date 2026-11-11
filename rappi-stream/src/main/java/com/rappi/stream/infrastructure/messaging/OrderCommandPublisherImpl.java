package com.rappi.stream.infrastructure.messaging;

import com.rappi.stream.application.OrderCommandPublisherPort;
import com.rappi.stream.domain.OrderCreatedCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class OrderCommandPublisherImpl implements OrderCommandPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    public OrderCommandPublisherImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarNuevoPedido(OrderCreatedCommand command, UUID orderId) {
        // Publicar el comando en la cola new_orders_queue
        // El message key es el orderId para correlacionamiento
        rabbitTemplate.convertAndSend("new_orders_queue", orderId.toString(), command);
    }
}