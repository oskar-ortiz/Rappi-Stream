package com.rappistream.dashboardaggregator.application;

import com.rappistream.dashboardaggregator.domain.EventType;
import com.rappistream.dashboardaggregator.domain.OrderEvent;
import com.rappistream.dashboardaggregator.domain.OrderView;
import com.rappistream.dashboardaggregator.domain.OrderViewSnapshot;
import com.rappistream.dashboardaggregator.domain.InvalidOrderTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderStateStore {
    private final ConcurrentHashMap<String, OrderView> orders = new ConcurrentHashMap<>();

    public OrderViewSnapshot apply(OrderEvent event) {
        OrderView updated = orders.compute(event.orderId(), (orderId, current) -> {
            if (current == null) {
                if (event.eventType() != EventType.DRIVER_ASSIGNED) {
                    throw new InvalidOrderTransitionException("First event for an order must be DRIVER_ASSIGNED");
                }
                return OrderView.assigned(event);
            }
            return current.advance(event);
        });
        return updated.snapshot();
    }

    public Collection<OrderViewSnapshot> snapshot() {
        List<OrderViewSnapshot> result = orders.values().stream()
                .map(OrderView::snapshot)
                .sorted(Comparator.comparing(OrderViewSnapshot::orderId))
                .toList();
        return List.copyOf(result);
    }

    public void clear() {
        orders.clear();
    }
}
