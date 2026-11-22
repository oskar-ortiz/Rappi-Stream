package com.rappistream.orderapi.domain;

/**
 * Raised while deserializing an order request whose values violate the request
 * invariants. Carries the offending field and a public, non-technical reason so
 * the web layer can build a clean validation error without exposing Jackson
 * internals.
 */
public class InvalidOrderRequestException extends IllegalArgumentException {

    private final String field;
    private final String reason;

    public InvalidOrderRequestException(String field, String reason) {
        super(field + " " + reason);
        this.field = field;
        this.reason = reason;
    }

    public String field() {
        return field;
    }

    public String reason() {
        return reason;
    }
}
