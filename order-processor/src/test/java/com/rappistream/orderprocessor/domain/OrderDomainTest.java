package com.rappistream.orderprocessor.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import com.rappistream.orderprocessor.application.SimpleDriverAssignmentStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateTest {

    @Test
    void createdCanAssignDriver() {
        // Arrange
        OrderState state = OrderState.CREATED;
        // Act
        OrderState next = state.assignDriver();
        // Assert
        assertEquals(OrderState.DRIVER_ASSIGNED, next);
    }

    @Test
    void assignedCannotAssignAgain() {
        // Arrange
        // Act
        // Assert
        assertThrows(InvalidStateTransitionException.class, () -> OrderState.DRIVER_ASSIGNED.assignDriver());
    }
}

class OrderTest {

    @Test
    void createStartsInCreated() {
        // Arrange
        // Act
        Order order = Order.create("o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), java.time.Instant.now());
        // Assert
        assertEquals(OrderState.CREATED, order.state());
    }

    @Test
    void assignDriverTransitions() {
        // Arrange
        Order order = Order.create("o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), java.time.Instant.now());
        // Act
        order.assignDriver("driver-1");
        // Assert
        assertEquals(OrderState.DRIVER_ASSIGNED, order.state());
        assertEquals("driver-1", order.driverId());
    }

    @Test
    void doubleAssignThrows() {
        // Arrange
        Order order = Order.create("o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), java.time.Instant.now());
        order.assignDriver("driver-1");
        // Act
        // Assert
        assertThrows(InvalidStateTransitionException.class, () -> order.assignDriver("driver-2"));
    }
}

class StrategyTest {

    @Test
    void simpleStrategyAssignsDriver() {
        // Arrange
        Order order = Order.create("o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), java.time.Instant.now());
        SimpleDriverAssignmentStrategy strategy = new SimpleDriverAssignmentStrategy();
        // Act
        String driverId = strategy.assignDriver(order);
        // Assert
        assertEquals(order.driverId(), driverId);
        assertEquals(OrderState.DRIVER_ASSIGNED, order.state());
    }
}
