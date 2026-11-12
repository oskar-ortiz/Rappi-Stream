package com.rappistream.orderprocessor.ports;

import com.rappistream.orderprocessor.domain.Order;

import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(String orderId);
}
