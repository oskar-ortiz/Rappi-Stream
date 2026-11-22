package com.rappistream.notificationrouter.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record NotificationPayload(
        @JsonProperty("notification_id")
        String notificationId,
        @JsonProperty("order_id")
        String orderId,
        @JsonProperty("recipient_type")
        String recipientType,
        @JsonProperty("recipient_id")
        String recipientId,
        String channel,
        String message,
        @JsonProperty("created_at")
        Instant createdAt
) {
    public NotificationPayload {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId is required");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (recipientType == null || recipientType.isBlank()) {
            throw new IllegalArgumentException("recipientType is required");
        }
        if (recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
