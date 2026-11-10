package com.rappistream.dashboardbackend.application;

import com.rappistream.dashboardbackend.domain.OrderStatus;
import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.DashboardClientsPort;
import com.rappistream.dashboardbackend.ports.OrderSnapshotStorePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyOrderStatusUpdateUseCaseTest {
    @Mock
    private OrderSnapshotStorePort snapshotStore;
    @Mock
    private DashboardClientsPort dashboardClients;
    @InjectMocks
    private ApplyOrderStatusUpdateUseCase useCase;

    @Test
    void broadcastsStoredUpdate() {
        OrderStatusUpdate update = update();
        when(snapshotStore.upsert(update)).thenReturn(true);

        useCase.apply(update);

        verify(snapshotStore).upsert(update);
        verify(dashboardClients).broadcast(update);
    }

    @Test
    void doesNotBroadcastStaleUpdate() {
        OrderStatusUpdate update = update();
        when(snapshotStore.upsert(update)).thenReturn(false);

        useCase.apply(update);

        verify(dashboardClients, never()).broadcast(update);
    }

    private OrderStatusUpdate update() {
        return new OrderStatusUpdate("order-1", OrderStatus.EN_CAMINO, "driver-1", Instant.EPOCH);
    }
}
