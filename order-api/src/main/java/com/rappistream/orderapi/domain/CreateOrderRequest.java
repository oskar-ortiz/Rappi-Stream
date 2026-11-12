package com.rappistream.orderapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @JsonProperty("customer_id")
        @NotBlank(message = "must not be blank")
        @Size(max = 64, message = "must be at most 64 characters")
        String customerId,
        @JsonProperty("customer_phone")
        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "must match ^\\+[1-9]\\d{7,14}$")
        String customerPhone,
        @JsonProperty("address")
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must be at most 200 characters")
        String address,
        @JsonProperty("items")
        @NotNull(message = "must not be null")
        @NotEmpty(message = "must contain between 1 and 50 items")
        @Size(min = 1, max = 50, message = "must contain between 1 and 50 items")
        @Valid
        List<OrderItem> items) {

    public CreateOrderRequest {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (customerId.length() > 64) {
            throw new IllegalArgumentException("customerId must be at most 64 characters");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new IllegalArgumentException("customerPhone must not be blank");
        }
        if (!customerPhone.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("customerPhone must match ^\\+[1-9]\\d{7,14}$");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
        if (address.length() > 200) {
            throw new IllegalArgumentException("address must be at most 200 characters");
        }
        if (items == null || items.isEmpty() || items.size() > 50) {
            throw new IllegalArgumentException("items must contain between 1 and 50 elements");
        }
        if (items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("items must not contain null elements");
        }
    }
}
