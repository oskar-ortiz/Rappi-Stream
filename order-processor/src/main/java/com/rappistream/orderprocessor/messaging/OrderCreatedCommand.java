package com.rappistream.orderprocessor.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rappistream.orderprocessor.domain.OrderItem;

import java.time.Instant;
import java.util.List;

public record OrderCreatedCommand(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("customer_phone") String customerPhone,
        @JsonProperty("address") String address,
        @JsonProperty("items") List<OrderItem> items,
        @JsonProperty("created_at") Instant createdAt) {

    public OrderCreatedCommand {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new IllegalArgumentException("customerPhone must not be blank");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
        if (items == null || items.isEmpty() || items.size() > 50) {
            throw new IllegalArgumentException("items must contain between 1 and 50 elements");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }
}
