package com.rappistream.notificationrouter.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rappistream.notificationrouter.ports.NotificationCommandPort;
import com.rappistream.notificationrouter.strategy.NotificationCommandResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NotificationRouterService {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationCommandResolver notificationCommandResolver;
    private final String exchange;

    public NotificationRouterService(RabbitTemplate rabbitTemplate,
                                     NotificationCommandResolver notificationCommandResolver,
                                     @Value("${app.rabbitmq.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.notificationCommandResolver = notificationCommandResolver;
        this.exchange = exchange;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onEvent(OrderEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("order event is required");
        }

        NotificationCommandPort commandPort = notificationCommandResolver.resolve(event);
        var payload = commandPort.toPayload(event);
        rabbitTemplate.convertAndSend(exchange, commandPort.routingKey(), payload);
    }

    public record OrderEvent(
            @JsonProperty("event_type") String eventType,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("driver_id") String driverId,
            @JsonProperty("occurred_at") Instant occurredAt
    ) {
        public OrderEvent {
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("eventType is required");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required");
            }
            if (occurredAt == null) {
                throw new IllegalArgumentException("occurredAt is required");
            }
        }
    }
}
