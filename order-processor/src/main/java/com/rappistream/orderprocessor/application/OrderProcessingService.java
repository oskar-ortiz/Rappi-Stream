package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.domain.OrderState;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import com.rappistream.orderprocessor.messaging.OrderEvent;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

public class OrderProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingService.class);

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

    /**
     * Idempotent processing of a new-order command.
     *
     * RabbitMQ delivers each command at least once, so redeliveries must be safe:
     * - Order not found: persist it as CREATED (publication intent), publish DRIVER_ASSIGNED,
     *   and only then persist the DRIVER_ASSIGNED state.
     * - Order found in CREATED: a previous delivery persisted it but its publication was not
     *   confirmed (for example during a Kafka outage); publish and mark DRIVER_ASSIGNED.
     * - Order found in DRIVER_ASSIGNED: the event was already published on an earlier
     *   delivery; do not publish again so the caller can ack the redelivered message.
     */
    public void process(OrderCreatedCommand command) {
        Order existing = orderRepository.findById(command.orderId()).orElse(null);
        if (existing != null && existing.state() == OrderState.DRIVER_ASSIGNED) {
            LOGGER.warn("Duplicate delivery for order {}: already DRIVER_ASSIGNED, skipping re-publication",
                    command.orderId());
            return;
        }
        if (existing != null) {
            LOGGER.info("Republishing DRIVER_ASSIGNED for order {} whose previous publication was not confirmed",
                    command.orderId());
        }
        Order order = existing != null
                ? existing
                : orderRepository.save(Order.create(
                        command.orderId(),
                        command.customerId(),
                        command.customerPhone(),
                        command.address(),
                        command.items().stream()
                                .map(i -> new OrderItem(i.sku(), i.quantity()))
                                .toList(),
                        command.createdAt()));
        driverAssignmentStrategy.assignDriver(order);
        OrderEvent event = new OrderEvent(
                "DRIVER_ASSIGNED",
                order.orderId(),
                order.driverId(),
                Instant.now());
        eventPublisher.publish(event);
        orderRepository.save(order);
    }
}
