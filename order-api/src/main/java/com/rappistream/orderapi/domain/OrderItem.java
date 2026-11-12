package com.rappistream.orderapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItem(
        @JsonProperty("sku") String sku,
        @JsonProperty("quantity") int quantity) {

    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (sku.length() > 64) {
            throw new IllegalArgumentException("sku must be at most 64 characters");
        }
        if (quantity < 1 || quantity > 99) {
            throw new IllegalArgumentException("quantity must be between 1 and 99");
        }
    }
}
