package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class OrderProcessingService {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;
    private final DriverAssignmentStrategy driverAssignmentStrategy;

    public OrderProcessingService(OrderRepositoryPort orderRepository,
                                 OrderEventPublisherPort eventPublisher,
                                 DriverAssignmentStrategy driverAssignmentStrategy) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.driverAssignmentStrategy = Objects.requireNonNull(driverAssignmentStrategy, "driverAssignmentStrategy");
    }

    public void process(OrderCreatedCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseGet(() -> {
                    Order created = Order.create(
                            command.orderId(),
                            command.customerId(),
                            command.customerPhone(),
                            command.address(),
                            command.items().stream()
                                    .map(i -> new OrderItem(i.sku(), i.quantity()))
                                    .toList(),
                            command.createdAt());
                    driverAssignmentStrategy.assignDriver(created);
                    return orderRepository.save(created);
                });
        com.rappistream.orderprocessor.messaging.OrderEvent event =
                new com.rappistream.orderprocessor.messaging.OrderEvent(
                        "DRIVER_ASSIGNED",
                        order.orderId(),
                        order.driverId(),
                        Instant.now());
        eventPublisher.publish(event);
    }
}
