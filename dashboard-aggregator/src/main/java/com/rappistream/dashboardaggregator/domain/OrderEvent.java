package com.rappistream.dashboardaggregator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderEvent(
        @JsonProperty("event_type") EventType eventType,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("driver_id") String driverId,
        @JsonProperty("occurred_at") Instant occurredAt) {

    public OrderEvent {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (eventType == EventType.DRIVER_ASSIGNED && (driverId == null || driverId.isBlank())) {
            throw new IllegalArgumentException("driverId is required for DRIVER_ASSIGNED");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }
}
