package com.rappistream.pushnotificationworker.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record NotificationPayload(
        @JsonProperty("notification_id") String notificationId,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("recipient_type") String recipientType,
        @JsonProperty("recipient_id") String recipientId,
        String channel,
        String message,
        @JsonProperty("created_at") Instant createdAt
) {
}
