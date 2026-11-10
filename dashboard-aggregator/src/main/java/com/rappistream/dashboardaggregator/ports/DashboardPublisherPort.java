package com.rappistream.dashboardaggregator.ports;

import com.rappistream.dashboardaggregator.domain.OrderViewSnapshot;

import java.util.Collection;

public interface DashboardPublisherPort {
    void publish(OrderViewSnapshot snapshot);

    void publishSnapshot(Collection<OrderViewSnapshot> snapshots);
}
