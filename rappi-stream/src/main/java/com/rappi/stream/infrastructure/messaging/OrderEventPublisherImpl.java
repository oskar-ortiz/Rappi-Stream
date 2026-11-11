package com.rappi.stream.infrastructure.messaging;

import com.rappi.stream.application.OrderEventPublisherPort;
import com.rappi.stream.domain.DriverAssignedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class OrderEventPublisherImpl implements OrderEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisherImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarDriverAssigned(DriverAssignedEvent event) {
        // Publicar en el topic order_events con order_id como key
        rabbitTemplate.convertAndSend("order_events", event.getOrderId(), event);
    }
}