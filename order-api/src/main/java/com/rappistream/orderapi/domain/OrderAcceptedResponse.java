package com.rappistream.orderapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderAcceptedResponse(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status") String status,
        @JsonProperty("received_at") Instant receivedAt) {

    public OrderAcceptedResponse {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt must not be null");
        }
    }
}
