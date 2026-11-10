package com.rappistream.smsworker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsWorkerServiceTest {
    @Mock
    private SmsGateway smsGateway;

    @InjectMocks
    private SmsWorkerService service;

    @Test
    void shouldSendReceivedNotificationThroughSmsGateway() {
        NotificationPayload payload = new NotificationPayload(
                "notification-1", "order-1", "client", "customer-1", "sms", "On the way", Instant.now());

        service.receive(payload);

        verify(smsGateway).send(payload);
    }
}
