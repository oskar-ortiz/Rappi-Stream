package com.rappistream.orderapi.infrastructure.config;

import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.ports.NewOrderCommandPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderApiConfiguration {

    @Bean
    public CreateOrderService createOrderService(NewOrderCommandPort newOrderCommandPort) {
        return new CreateOrderService(newOrderCommandPort);
    }
}
