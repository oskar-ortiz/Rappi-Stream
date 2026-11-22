package com.rappistream.orderprocessor;

import com.rappistream.orderprocessor.application.OrderProcessingService;
import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.infrastructure.amqp.OrderCommandRabbitListener;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "management.endpoint.health.group.readiness.include=rabbitListener,kafka"
})
class OrderProcessorApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private OrderRepositoryPort orderRepositoryPort;

    @MockBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @MockBean
    private OrderCommandRabbitListener orderCommandRabbitListener;

    @Test
    void applicationContextContainsProcessingServiceAndStrategy() {
        assertNotNull(applicationContext.getBean(OrderProcessingService.class));
        assertNotNull(applicationContext.getBean(DriverAssignmentStrategy.class));
    }
}
