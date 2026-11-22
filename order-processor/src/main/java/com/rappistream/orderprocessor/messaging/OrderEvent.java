package com.rappistream.orderprocessor.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderEvent(
        @JsonProperty("event_type")
        String eventType,
        @JsonProperty("order_id")
        String orderId,
        @JsonProperty("driver_id")
        String driverId,
        @JsonProperty("occurred_at")
        Instant occurredAt
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
