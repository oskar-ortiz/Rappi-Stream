package com.rappistream.dashboardbackend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusUpdateTest {
    @Test
    void failsFastForInvalidFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderStatusUpdate(" ", OrderStatus.EN_CAMINO, "driver-1", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderStatusUpdate("order-1", null, "driver-1", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderStatusUpdate("order-1", OrderStatus.EN_CAMINO, " ", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderStatusUpdate("order-1", OrderStatus.EN_CAMINO, "driver-1", null));
    }
}
