package com.rappi.stream.domain;

import lombok.Value;

@Value
public class DriverAssignedEvent {
    private String orderId;
    private String driverId;
    private String timestamp;

    public DriverAssignedEvent {
        validarOrderId();
        validarDriverId();
    }

    private void validarOrderId() {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("El orderId no puede ser nulo o estar en blanco");
        }
    }

    private void validarDriverId() {
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("El driverId no puede ser nulo o estar en blanco");
        }
    }
}