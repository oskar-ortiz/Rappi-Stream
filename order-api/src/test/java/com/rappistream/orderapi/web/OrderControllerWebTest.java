package com.rappistream.orderapi.web;

import com.rappistream.orderapi.application.BrokerUnavailableException;
import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.domain.OrderAcceptedResponse;
import com.rappistream.orderapi.infrastructure.web.OrderController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderService createOrderService;

    @Test
    void validRequestReturns202() throws Exception {
        // Arrange
        when(createOrderService.create(any())).thenReturn(
                new OrderAcceptedResponse("550e8400-e29b-41d4-a716-446655440000", "RECEIVED", Instant.now()));
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"+14155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":2}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        // Arrange
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"4155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":0}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        // Arrange
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        // Arrange
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void brokerFailureReturns503() throws Exception {
        // Arrange
        when(createOrderService.create(any())).thenThrow(new BrokerUnavailableException("down"));
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"+14155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":2}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void unexpectedFailureReturns500() throws Exception {
        // Arrange
        when(createOrderService.create(any())).thenThrow(new RuntimeException("boom"));
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"+14155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":2}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError());
    }
}
