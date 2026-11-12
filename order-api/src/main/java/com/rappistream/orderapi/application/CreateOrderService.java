package com.rappistream.orderapi.application;

import com.rappistream.orderapi.domain.CreateOrderRequest;
import com.rappistream.orderapi.domain.OrderAcceptedResponse;
import com.rappistream.orderapi.domain.OrderCreatedCommand;
import com.rappistream.orderapi.domain.OrderItem;
import com.rappistream.orderapi.ports.NewOrderCommandPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateOrderService {

    private final NewOrderCommandPort newOrderCommandPort;

    public CreateOrderService(NewOrderCommandPort newOrderCommandPort) {
        this.newOrderCommandPort = newOrderCommandPort;
    }

    public OrderAcceptedResponse create(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        List<OrderItem> items = request.items().stream()
                .map(item -> new OrderItem(item.sku(), item.quantity()))
                .toList();
        OrderCreatedCommand command = new OrderCreatedCommand(
                orderId,
                request.customerId(),
                request.customerPhone(),
                request.address(),
                items,
                createdAt);
        newOrderCommandPort.publish(command);
        return new OrderAcceptedResponse(orderId, "RECEIVED", createdAt);
    }
}
