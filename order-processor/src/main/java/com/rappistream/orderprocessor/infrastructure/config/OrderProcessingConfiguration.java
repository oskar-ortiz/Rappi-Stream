package com.rappistream.orderprocessor.infrastructure.config;

import com.rappistream.orderprocessor.application.OrderProcessingService;
import com.rappistream.orderprocessor.application.SimpleDriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderProcessingConfiguration {

    @Bean
    public OrderProcessingService orderProcessingService(
            OrderRepositoryPort orderRepositoryPort,
            OrderEventPublisherPort orderEventPublisherPort,
            DriverAssignmentStrategy driverAssignmentStrategy) {
        return new OrderProcessingService(
                orderRepositoryPort,
                orderEventPublisherPort,
                driverAssignmentStrategy);
    }

    @Bean
    public DriverAssignmentStrategy driverAssignmentStrategy() {
        return new SimpleDriverAssignmentStrategy();
    }
}
