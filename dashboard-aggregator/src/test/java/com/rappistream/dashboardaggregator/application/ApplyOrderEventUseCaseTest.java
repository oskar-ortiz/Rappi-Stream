package com.rappistream.dashboardaggregator.application;

import com.rappistream.dashboardaggregator.domain.EventType;
import com.rappistream.dashboardaggregator.domain.InvalidOrderTransitionException;
import com.rappistream.dashboardaggregator.domain.OrderEvent;
import com.rappistream.dashboardaggregator.domain.OrderStatus;
import com.rappistream.dashboardaggregator.domain.OrderViewSnapshot;
import com.rappistream.dashboardaggregator.ports.DashboardPublisherPort;
import com.rappistream.dashboardaggregator.ports.OrderHistoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyOrderEventUseCaseTest {
    @Mock
    private OrderStateStore stateStore;
    @Mock
    private DashboardPublisherPort dashboardPublisher;
    @Mock
    private OrderHistoryPort orderHistory;
    @InjectMocks
    private ApplyOrderEventUseCase useCase;

    @Test
    void recordsAndPublishesAfterValidEvent() {
        OrderEvent event = new OrderEvent(EventType.DRIVER_ASSIGNED,
                "order-1", "driver-1", Instant.parse("2026-01-01T00:00:00Z"));
        OrderViewSnapshot snapshot = new OrderViewSnapshot(
                "order-1", OrderStatus.EN_CAMINO, "driver-1", event.occurredAt());
        when(stateStore.apply(event)).thenReturn(snapshot);

        useCase.consume(event);

        verify(orderHistory).record(event);
        verify(dashboardPublisher).publish(snapshot);
    }

    @Test
    void invalidTransitionDoesNotInvokeDownstreamPorts() {
        OrderEvent event = new OrderEvent(EventType.ORDER_DELIVERED,
                "order-1", null, Instant.parse("2026-01-01T00:00:00Z"));
        when(stateStore.apply(event)).thenThrow(new InvalidOrderTransitionException("invalid"));

        assertThrows(InvalidOrderTransitionException.class, () -> useCase.consume(event));

        verifyNoInteractions(orderHistory, dashboardPublisher);
    }
}
