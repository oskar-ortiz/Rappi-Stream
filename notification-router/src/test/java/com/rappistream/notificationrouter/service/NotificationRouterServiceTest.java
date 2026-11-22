package com.rappistream.notificationrouter.service;

import com.rappistream.notificationrouter.domain.NotificationPayload;
import com.rappistream.notificationrouter.strategy.DriverPushNotificationCommand;
import com.rappistream.notificationrouter.strategy.NotificationCommandResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class NotificationRouterServiceTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private NotificationCommandResolver notificationCommandResolver;

    private NotificationRouterService service;

    @BeforeEach
    void setUp() {
        service = new NotificationRouterService(rabbitTemplate, notificationCommandResolver, "configured-notifications");
    }

    @Test
    void shouldPublishNotificationToRabbitMqUsingResolvedRoutingKey() {
        DriverPushNotificationCommand command = new DriverPushNotificationCommand();
        when(notificationCommandResolver.resolve(any())).thenReturn(command);

        NotificationRouterService.OrderEvent event =
                new NotificationRouterService.OrderEvent("DRIVER_ASSIGNED", "order-1", "driver-1", Instant.now());

        service.onEvent(event);

        verify(rabbitTemplate).convertAndSend(eq("configured-notifications"), eq("notify.driver.push"), any(NotificationPayload.class));
    }

    @Test
    void shouldRejectNullEventsWithoutPublishing() {
        assertThrows(IllegalArgumentException.class, () -> service.onEvent(null));

        verifyNoInteractions(notificationCommandResolver, rabbitTemplate);
    }
}
