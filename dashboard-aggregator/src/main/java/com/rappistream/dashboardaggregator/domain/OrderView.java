package com.rappistream.dashboardaggregator.domain;

import java.util.Objects;

public final class OrderView {
    private final String orderId;
    private final String driverId;
    private final OrderStatus status;
    private final java.time.Instant timestamp;

    private OrderView(String orderId, String driverId, OrderStatus status, java.time.Instant timestamp) {
        this.orderId = orderId;
        this.driverId = driverId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public static OrderView assigned(OrderEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.eventType() != EventType.DRIVER_ASSIGNED) {
            throw new InvalidOrderTransitionException("An order view must start with DRIVER_ASSIGNED");
        }
        return new OrderView(event.orderId(), event.driverId(), OrderStatus.EN_CAMINO, event.occurredAt());
    }

    public OrderView advance(OrderEvent event) {
        Objects.requireNonNull(event, "event");
        if (!orderId.equals(event.orderId())) {
            throw new InvalidOrderTransitionException("Event orderId does not match this order view");
        }
        if (event.eventType() == EventType.DRIVER_ASSIGNED) {
            throw new InvalidOrderTransitionException("DRIVER_ASSIGNED cannot be repeated");
        }
        return new OrderView(orderId, driverId, status.after(event.eventType()), event.occurredAt());
    }

    public OrderViewSnapshot snapshot() {
        return new OrderViewSnapshot(orderId, status, driverId, timestamp);
    }
}
