package com.rappistream.dashboardaggregator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderViewTest {
    @Test
    void appliesLifecycleInContractOrder() {
        OrderView assigned = OrderView.assigned(event(EventType.DRIVER_ASSIGNED, "driver-1", 1));

        OrderView arriving = assigned.advance(event(EventType.DRIVER_ARRIVING, null, 2));
        OrderView delivered = arriving.advance(event(EventType.ORDER_DELIVERED, null, 3));

        assertEquals(OrderStatus.EN_CAMINO, assigned.snapshot().status());
        assertEquals(OrderStatus.LLEGANDO, arriving.snapshot().status());
        assertEquals(OrderStatus.ENTREGADO, delivered.snapshot().status());
        assertEquals("driver-1", delivered.snapshot().driverId());
    }

    @Test
    void rejectsRepeatedOrSkippedTransitions() {
        OrderView assigned = OrderView.assigned(event(EventType.DRIVER_ASSIGNED, "driver-1", 1));

        assertThrows(InvalidOrderTransitionException.class,
                () -> assigned.advance(event(EventType.ORDER_DELIVERED, null, 2)));
        assertThrows(InvalidOrderTransitionException.class,
                () -> assigned.advance(event(EventType.DRIVER_ASSIGNED, "driver-1", 2)));
    }

    private OrderEvent event(EventType type, String driverId, long second) {
        return new OrderEvent(type, "order-1", driverId, Instant.ofEpochSecond(second));
    }
}
