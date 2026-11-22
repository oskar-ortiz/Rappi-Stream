package com.rappistream.orderapi.ports;

import com.rappistream.orderapi.domain.OrderCreatedCommand;

public interface NewOrderCommandPort {
    void publish(OrderCreatedCommand command);
}
