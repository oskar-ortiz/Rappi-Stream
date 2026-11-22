package com.rappistream.dashboardaggregator.application;

import com.rappistream.dashboardaggregator.domain.OrderEvent;
import com.rappistream.dashboardaggregator.domain.OrderViewSnapshot;
import com.rappistream.dashboardaggregator.ports.DashboardPublisherPort;
import com.rappistream.dashboardaggregator.ports.OrderEventsConsumerPort;
import com.rappistream.dashboardaggregator.ports.OrderHistoryPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ApplyOrderEventUseCase implements OrderEventsConsumerPort {
    private final OrderStateStore stateStore;
    private final DashboardPublisherPort dashboardPublisher;
    private final OrderHistoryPort orderHistory;

    public ApplyOrderEventUseCase(
            OrderStateStore stateStore,
            DashboardPublisherPort dashboardPublisher,
            OrderHistoryPort orderHistory) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.dashboardPublisher = Objects.requireNonNull(dashboardPublisher, "dashboardPublisher");
        this.orderHistory = Objects.requireNonNull(orderHistory, "orderHistory");
    }

    @Override
    public void consume(OrderEvent event) {
        OrderViewSnapshot snapshot = stateStore.apply(event);
        orderHistory.record(event);
        dashboardPublisher.publish(snapshot);
    }
}
