package com.rappistream.dashboardbackend.application;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.OrderSnapshotStorePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderSnapshotService {
    private final OrderSnapshotStorePort snapshotStore;

    public OrderSnapshotService(OrderSnapshotStorePort snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    public List<OrderStatusUpdate> getSnapshot() {
        return snapshotStore.snapshot();
    }
}
