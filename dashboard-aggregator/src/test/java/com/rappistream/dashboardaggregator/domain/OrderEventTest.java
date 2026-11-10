package com.rappistream.dashboardaggregator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderEventTest {
    @Test
    void failsFastForMissingFieldsAndAssignedDriver() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderEvent(null, "order-1", null, Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderEvent(EventType.DRIVER_ASSIGNED, " ", "driver-1", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderEvent(EventType.DRIVER_ASSIGNED, "order-1", " ", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderEvent(EventType.DRIVER_ARRIVING, "order-1", null, null));
    }
}
