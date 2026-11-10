package com.rappistream.dashboardaggregator.application;

import com.rappistream.dashboardaggregator.domain.EventType;
import com.rappistream.dashboardaggregator.domain.InvalidOrderTransitionException;
import com.rappistream.dashboardaggregator.domain.OrderEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateStoreTest {
    @Test
    void rejectedEventLeavesMaterializedViewUnchanged() {
        OrderStateStore store = new OrderStateStore();
        store.apply(new OrderEvent(EventType.DRIVER_ASSIGNED, "order-1", "driver-1", Instant.EPOCH));

        assertThrows(InvalidOrderTransitionException.class, () -> store.apply(
                new OrderEvent(EventType.ORDER_DELIVERED, "order-1", null, Instant.EPOCH.plusSeconds(1))));

        assertEquals(1, store.snapshot().size());
        assertEquals("EN_CAMINO", store.snapshot().iterator().next().status().name());
    }
}
