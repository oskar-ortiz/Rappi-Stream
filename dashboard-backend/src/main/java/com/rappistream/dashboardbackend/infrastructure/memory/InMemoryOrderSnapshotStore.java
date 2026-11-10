package com.rappistream.dashboardbackend.infrastructure.memory;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.OrderSnapshotStorePort;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
public class InMemoryOrderSnapshotStore implements OrderSnapshotStorePort {
    private final ConcurrentHashMap<String, OrderStatusUpdate> updates = new ConcurrentHashMap<>();

    @Override
    public boolean upsert(OrderStatusUpdate update) {
        AtomicBoolean changed = new AtomicBoolean();
        updates.compute(update.orderId(), (orderId, current) -> {
            if (current == null || update.timestamp().isAfter(current.timestamp())) {
                changed.set(true);
                return update;
            }
            return current;
        });
        return changed.get();
    }

    @Override
    public List<OrderStatusUpdate> snapshot() {
        return updates.values().stream()
                .sorted(Comparator.comparing(OrderStatusUpdate::orderId))
                .toList();
    }
}
