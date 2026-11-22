package com.rappistream.dashboardbackend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderStatusUpdate(
        @JsonProperty("order_id") String orderId,
        OrderStatus status,
        @JsonProperty("driver_id") String driverId,
        Instant timestamp) {

    public OrderStatusUpdate {
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
