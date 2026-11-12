package com.rappistream.orderprocessor.ports;

import com.rappistream.orderprocessor.messaging.OrderEvent;

public interface OrderEventPublisherPort {
    void publish(OrderEvent event);
}
