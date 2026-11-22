package com.rappistream.dashboardaggregator.domain;

public final class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
