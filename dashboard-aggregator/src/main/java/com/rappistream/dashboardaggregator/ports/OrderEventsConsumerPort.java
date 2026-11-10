package com.rappistream.dashboardaggregator.ports;

import com.rappistream.dashboardaggregator.domain.OrderEvent;

public interface OrderEventsConsumerPort {
    void consume(OrderEvent event);
}
