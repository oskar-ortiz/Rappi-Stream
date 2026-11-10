package com.rappistream.dashboardaggregator.ports;

import com.rappistream.dashboardaggregator.domain.OrderEvent;

public interface OrderHistoryPort {
    void record(OrderEvent event);
}
