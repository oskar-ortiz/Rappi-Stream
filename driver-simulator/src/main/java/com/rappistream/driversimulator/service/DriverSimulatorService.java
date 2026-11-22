package com.rappistream.driversimulator.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DriverSimulatorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverSimulatorService.class);

    private final KafkaTemplate<String, DriverEvent> kafkaTemplate;
    private final ScheduledExecutorService executor;
    private final String orderEventsTopic;
    private final long arrivingDelaySeconds;
    private final long deliveredDelaySeconds;

    public DriverSimulatorService(KafkaTemplate<String, DriverEvent> kafkaTemplate,
                                  ScheduledExecutorService driverTripScheduler,
                                  @Value("${app.kafka.topic}") String orderEventsTopic,
                                  @Value("${app.driver.arriving-delay-seconds}") long arrivingDelaySeconds,
                                  @Value("${app.driver.delivered-delay-seconds}") long deliveredDelaySeconds) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = driverTripScheduler;
        this.orderEventsTopic = orderEventsTopic;
        this.arrivingDelaySeconds = arrivingDelaySeconds;
        this.deliveredDelaySeconds = deliveredDelaySeconds;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, DriverEvent> record) {
        DriverEvent event = record.value();
        if (!"DRIVER_ASSIGNED".equals(event.eventType())) {
            return;
        }

        scheduleTrip(event);
    }

    void scheduleTrip(DriverEvent assignment) {
        executor.schedule(() -> publishOrderEvent("DRIVER_ARRIVING", assignment), arrivingDelaySeconds, TimeUnit.SECONDS);
        executor.schedule(() -> publishOrderEvent("ORDER_DELIVERED", assignment), deliveredDelaySeconds, TimeUnit.SECONDS);
    }

    void publishOrderEvent(String eventType, DriverEvent assignment) {
        DriverEvent event = new DriverEvent(eventType, assignment.orderId(), assignment.driverId(), Instant.now());
        try {
            kafkaTemplate.send(orderEventsTopic, assignment.orderId(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("Failed to publish {} for order {}", eventType, assignment.orderId(), e);
        }
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
