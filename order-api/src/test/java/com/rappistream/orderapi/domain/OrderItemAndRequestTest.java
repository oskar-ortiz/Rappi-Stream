package com.rappistream.orderapi.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemTest {

    @Test
    void validItemDoesNotThrow() {
        // Arrange
        // Act
        // Assert
        assertDoesNotThrow(() -> new OrderItem("sku-1", 2));
    }

    @Test
    void blankSkuThrows() {
        // Arrange
        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(" ", 2));
    }

    @Test
    void quantityOutOfRangeThrows() {
        // Arrange
        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new OrderItem("sku-1", 0));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem("sku-1", 100));
    }
}

class CreateOrderRequestTest {

    @Test
    void validRequestDoesNotThrow() {
        // Arrange
        // Act
        // Assert
        assertDoesNotThrow(() -> new CreateOrderRequest(
                "customer-1", "+14155552671", "123 Market", List.of(new OrderItem("sku-1", 2))));
    }

    @Test
    void invalidPhoneThrows() {
        // Arrange
        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new CreateOrderRequest(
                "customer-1", "4155552671", "123 Market", List.of(new OrderItem("sku-1", 2))));
    }

    @Test
    void emptyItemsThrows() {
        // Arrange
        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new CreateOrderRequest(
                "customer-1", "+14155552671", "123 Market", List.of()));
    }

    @Test
    void tooManyItemsThrows() {
        // Arrange
        List<OrderItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) items.add(new OrderItem("sku-" + i, 1));
        // Act
        // Assert
        assertThrows(IllegalArgumentException.class, () -> new CreateOrderRequest(
                "customer-1", "+14155552671", "123 Market", items));
    }
}
