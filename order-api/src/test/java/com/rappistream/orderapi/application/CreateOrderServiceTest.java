package com.rappistream.orderapi.application;

import com.rappistream.orderapi.domain.CreateOrderRequest;
import com.rappistream.orderapi.domain.OrderItem;
import com.rappistream.orderapi.ports.NewOrderCommandPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private NewOrderCommandPort newOrderCommandPort;

    @InjectMocks
    private CreateOrderService service;

    @Test
    void publishesCommandOnSuccess() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-1", "+14155552671", "123 Market", List.of(new OrderItem("sku-1", 2)));
        // Act
        var response = service.create(request);
        // Assert
        assertEquals("RECEIVED", response.status());
        verify(newOrderCommandPort).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void portFailurePropagatesAsBrokerUnavailable() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-1", "+14155552671", "123 Market", List.of(new OrderItem("sku-1", 2)));
        doThrow(new BrokerUnavailableException("down")).when(newOrderCommandPort).publish(org.mockito.ArgumentMatchers.any());
        // Act
        // Assert
        assertThrows(BrokerUnavailableException.class, () -> service.create(request));
        verify(newOrderCommandPort).publish(org.mockito.ArgumentMatchers.any());
    }
}
