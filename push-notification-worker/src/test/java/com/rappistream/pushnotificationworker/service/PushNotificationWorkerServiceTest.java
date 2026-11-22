package com.rappistream.pushnotificationworker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PushNotificationWorkerServiceTest {
    @Mock
    private PushNotificationGateway pushNotificationGateway;

    @InjectMocks
    private PushNotificationWorkerService service;

    @Test
    void shouldSendReceivedNotificationThroughPushGateway() {
        NotificationPayload payload = new NotificationPayload(
                "notification-1", "order-1", "driver", "driver-1", "push", "Driver assigned", Instant.now());

        service.receive(payload);

        verify(pushNotificationGateway, times(1)).send(payload);
    }
}
