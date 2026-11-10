package com.rappistream.dashboardaggregator.infrastructure.history;

import com.rappistream.dashboardaggregator.domain.OrderEvent;
import com.rappistream.dashboardaggregator.ports.OrderHistoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOrderHistoryAdapter implements OrderHistoryPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOrderHistoryAdapter.class);

    @Override
    public void record(OrderEvent event) {
        // TODO(Persona A): implementación JPA.
        LOGGER.info("Order event audit pending JPA persistence: orderId={}, type={}",
                event.orderId(), event.eventType());
    }
}
