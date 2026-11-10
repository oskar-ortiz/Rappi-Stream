package com.rappistream.dashboardbackend.ports;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;

import java.util.List;

public interface OrderSnapshotStorePort {
    boolean upsert(OrderStatusUpdate update);

    List<OrderStatusUpdate> snapshot();
}
