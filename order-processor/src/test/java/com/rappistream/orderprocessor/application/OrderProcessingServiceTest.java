package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @Mock
    private DriverAssignmentStrategy driverAssignmentStrategy;

    @Test
    void persistsThenPublishes() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        OrderCreatedCommand command = new OrderCreatedCommand(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());
        Order created = Order.create(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        when(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(created);
        // Act
        service.process(command);
        // Assert
        InOrder order = inOrder(orderRepository, eventPublisher);
        order.verify(orderRepository).save(org.mockito.ArgumentMatchers.any());
        order.verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishesEventWhenOrderAlreadyExists() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        OrderCreatedCommand command = new OrderCreatedCommand(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        Order existing = Order.create(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        existing.assignDriver("driver-existing");
        when(orderRepository.findById("o1")).thenReturn(Optional.of(existing));
        // Act
        service.process(command);
        // Assert
        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event.orderId().equals("o1") && event.driverId().equals("driver-existing")));
    }
}
