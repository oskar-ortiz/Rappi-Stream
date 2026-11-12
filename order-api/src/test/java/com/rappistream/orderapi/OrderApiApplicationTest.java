package com.rappistream.orderapi;

import com.rappistream.orderapi.application.CreateOrderService;
import com.rappistream.orderapi.ports.NewOrderCommandPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrderApiApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private NewOrderCommandPort newOrderCommandPort;

    @Test
    void applicationContextContainsCreateOrderService() {
        assertNotNull(applicationContext.getBean(CreateOrderService.class));
    }
}
