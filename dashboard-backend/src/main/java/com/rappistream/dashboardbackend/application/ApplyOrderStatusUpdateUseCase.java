package com.rappistream.dashboardbackend.application;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.DashboardClientsPort;
import com.rappistream.dashboardbackend.ports.OrderSnapshotStorePort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ApplyOrderStatusUpdateUseCase {
    private final OrderSnapshotStorePort snapshotStore;
    private final DashboardClientsPort dashboardClients;

    public ApplyOrderStatusUpdateUseCase(
            OrderSnapshotStorePort snapshotStore,
            DashboardClientsPort dashboardClients) {
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore");
        this.dashboardClients = Objects.requireNonNull(dashboardClients, "dashboardClients");
    }

    public void apply(OrderStatusUpdate update) {
        if (snapshotStore.upsert(update)) {
            dashboardClients.broadcast(update);
        }
    }
}
