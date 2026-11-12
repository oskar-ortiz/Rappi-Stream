package com.rappistream.orderapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("details") List<ErrorDetail> details,
        @JsonProperty("timestamp") Instant timestamp) {

    public ErrorResponse {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (details == null) {
            throw new IllegalArgumentException("details must not be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }
}

