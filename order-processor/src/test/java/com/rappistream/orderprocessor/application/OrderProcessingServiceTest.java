package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.domain.OrderState;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private OrderCreatedCommand command() {
        return new OrderCreatedCommand(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
    }

    private void assignDriverOnStrategy(String driverId) {
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.assignDriver(driverId);
            return driverId;
        }).when(driverAssignmentStrategy).assignDriver(any());
    }

    /**
     * Mirrors JpaOrderRepositoryAdapter, whose save() round-trips through a fresh
     * domain object instead of returning the same mutable instance.
     */
    private void snapshotOrderOnSave() {
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            Order snapshot = Order.create(order.orderId(), order.customerId(), order.customerPhone(),
                    order.address(), order.items(), order.createdAt());
            if (order.driverId() != null) {
                snapshot.assignDriver(order.driverId());
            }
            return snapshot;
        });
    }

    @Test
    void newOrderIsPersistedAsCreatedThenPublishedAndMarkedAssigned() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());
        snapshotOrderOnSave();
        assignDriverOnStrategy("driver-1");
        // Act
        service.process(command());
        // Assert
        InOrder order = inOrder(orderRepository, eventPublisher);
        order.verify(orderRepository).save(any(Order.class));
        order.verify(eventPublisher).publish(any());
        order.verify(orderRepository).save(any(Order.class));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void newOrderPersistsCreatedStateBeforePublishingAndAssignedStateAfterwards() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());
        snapshotOrderOnSave();
        assignDriverOnStrategy("driver-1");
        // Act
        service.process(command());
        // Assert
        verify(orderRepository).save(org.mockito.ArgumentMatchers.<Order>argThat(
                o -> o.state() == OrderState.CREATED && o.driverId() == null));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(
                e -> "DRIVER_ASSIGNED".equals(e.eventType())
                        && "o1".equals(e.orderId())
                        && "driver-1".equals(e.driverId())));
        verify(orderRepository).save(org.mockito.ArgumentMatchers.<Order>argThat(
                o -> o.state() == OrderState.DRIVER_ASSIGNED && "driver-1".equals(o.driverId())));
    }

    @Test
    void redeliveryAfterAssignmentDoesNotRepublish() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        Order existing = Order.create(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        existing.assignDriver("driver-existing");
        when(orderRepository.findById("o1")).thenReturn(Optional.of(existing));
        // Act
        service.process(command());
        // Assert
        verify(eventPublisher, never()).publish(any());
        verify(orderRepository, never()).save(any());
        verify(driverAssignmentStrategy, never()).assignDriver(any());
    }

    @Test
    void redeliveryAfterUnconfirmedPublicationRepublishesAndMarksAssigned() {
        // Arrange
        OrderProcessingService service = new OrderProcessingService(orderRepository, eventPublisher, driverAssignmentStrategy);
        Order pending = Order.create(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), Instant.now());
        when(orderRepository.findById("o1")).thenReturn(Optional.of(pending));
        snapshotOrderOnSave();
        assignDriverOnStrategy("driver-2");
        // Act
        service.process(command());
        // Assert
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(
                e -> "DRIVER_ASSIGNED".equals(e.eventType())
                        && "o1".equals(e.orderId())
                        && "driver-2".equals(e.driverId())));
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository).save(org.mockito.ArgumentMatchers.<Order>argThat(
                o -> o.state() == OrderState.DRIVER_ASSIGNED && "driver-2".equals(o.driverId())));
    }
}
