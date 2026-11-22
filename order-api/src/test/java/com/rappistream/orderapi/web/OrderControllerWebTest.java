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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void invalidPhoneReturnsCleanPublicValidationError() throws Exception {
        // Arrange
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"4155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":2}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("customerPhone"))
                .andExpect(jsonPath("$.details[0].reason").value("must match ^\\+[1-9]\\d{7,14}$"))
                .andExpect(jsonPath("$.details[0].reason").value(not(containsString("Source"))))
                .andExpect(jsonPath("$.details[0].reason").value(not(containsString("Cannot construct instance"))));
    }

    @Test
    void malformedJsonReturnsCleanPublicInvalidJsonError() throws Exception {
        // Arrange
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_JSON"))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"))
                .andExpect(jsonPath("$.details[0].field").value("$"))
                .andExpect(jsonPath("$.details[0].reason").value("malformed or empty JSON input"))
                .andExpect(content().string(not(containsString("Unrecognized token"))))
                .andExpect(content().string(not(containsString("JsonParser"))));
    }

    @Test
    void typeMismatchReturnsCleanFieldPath() throws Exception {
        // Arrange
        String body = "{\"customer_id\":\"customer-456\",\"customer_phone\":\"+14155552671\",\"address\":\"123 Market\",\"items\":[{\"sku\":\"sku-001\",\"quantity\":\"two\"}]}";
        // Act
        // Assert
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("items[0].quantity"))
                .andExpect(jsonPath("$.details[0].reason").value("value has an invalid type or format"))
                .andExpect(content().string(not(containsString("String value"))));
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
