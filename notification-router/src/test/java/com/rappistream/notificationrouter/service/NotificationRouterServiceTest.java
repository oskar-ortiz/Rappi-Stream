package com.rappistream.notificationrouter.service;

import com.rappistream.notificationrouter.domain.NotificationPayload;
import com.rappistream.notificationrouter.strategy.DriverPushNotificationCommand;
import com.rappistream.notificationrouter.strategy.NotificationCommandResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRouterServiceTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private NotificationCommandResolver notificationCommandResolver;

    @InjectMocks
    private NotificationRouterService service;

    @Test
    void shouldPublishNotificationToRabbitMqUsingResolvedRoutingKey() {
        DriverPushNotificationCommand command = new DriverPushNotificationCommand();
        when(notificationCommandResolver.resolve(any())).thenReturn(command);

        NotificationRouterService.OrderEvent event =
                new NotificationRouterService.OrderEvent("DRIVER_ASSIGNED", "order-1", "driver-1", Instant.now());

        service.onEvent(event);

        verify(rabbitTemplate).convertAndSend(eq(NotificationRouterService.EXCHANGE), eq("notify.driver.push"), any(NotificationPayload.class));
    }
}
