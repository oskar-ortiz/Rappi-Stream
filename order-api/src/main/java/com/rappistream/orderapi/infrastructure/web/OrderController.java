package com.rappistream.orderapi.infrastructure.web;

import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.domain.CreateOrderRequest;
import com.rappistream.orderapi.domain.OrderAcceptedResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OrderController {

    private final CreateOrderService createOrderService;

    public OrderController(CreateOrderService createOrderService) {
        this.createOrderService = createOrderService;
    }

    @PostMapping(value = "/order", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderAcceptedResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderAcceptedResponse response = createOrderService.create(request);
        URI location = URI.create("/order/" + response.orderId());
        return ResponseEntity.accepted().location(location).body(response);
    }
}
