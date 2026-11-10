package com.rappistream.driversimulator.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DriverSimulatorService {
    public static final String ORDER_EVENTS_TOPIC = "order_events";
    private final KafkaTemplate<String, DriverEvent> kafkaTemplate;
    private final ScheduledExecutorService executor;

    public DriverSimulatorService(KafkaTemplate<String, DriverEvent> kafkaTemplate,
                                 ScheduledExecutorService driverTripScheduler) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = driverTripScheduler;
    }

    @KafkaListener(topics = "order_events", groupId = "driver-simulator-group")
    public void listen(ConsumerRecord<String, DriverEvent> record) {
        DriverEvent event = record.value();
        if (!"DRIVER_ASSIGNED".equals(event.eventType())) {
            return;
        }

        scheduleTrip(event);
    }

    void scheduleTrip(DriverEvent assignment) {
        executor.schedule(() -> publishOrderEvent("DRIVER_ARRIVING", assignment), 5, TimeUnit.SECONDS);
        executor.schedule(() -> publishOrderEvent("ORDER_DELIVERED", assignment), 10, TimeUnit.SECONDS);
    }

    void publishOrderEvent(String eventType, DriverEvent assignment) {
        DriverEvent event = new DriverEvent(eventType, assignment.orderId(), assignment.driverId(), Instant.now());
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, assignment.orderId(), event);
    }

    public record DriverEvent(
            @JsonProperty("event_type")
            String eventType,
            @JsonProperty("order_id")
            String orderId,
            @JsonProperty("driver_id")
            String driverId,
            @JsonProperty("occurred_at")
            Instant occurredAt
    ) {
        public DriverEvent {
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
