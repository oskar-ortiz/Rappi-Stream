package com.rappistream.dashboardaggregator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderViewSnapshot(
        @JsonProperty("order_id") String orderId,
        OrderStatus status,
        @JsonProperty("driver_id") String driverId,
        @JsonProperty("timestamp") Instant timestamp) {

    public OrderViewSnapshot {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException("driverId must not be blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
    }
}
