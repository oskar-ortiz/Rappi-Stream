package com.rappistream.notificationrouter.strategy;

import com.rappistream.notificationrouter.domain.NotificationPayload;
import com.rappistream.notificationrouter.ports.NotificationCommandPort;
import com.rappistream.notificationrouter.service.NotificationRouterService.OrderEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ClientSmsNotificationCommand implements NotificationCommandPort {
    @Override
    public boolean supports(String eventType) {
        return "DRIVER_ARRIVING".equals(eventType) || "ORDER_DELIVERED".equals(eventType);
    }

    @Override
    public String routingKey() {
        return "notify.client.sms";
    }

    @Override
    public NotificationPayload toPayload(OrderEvent event) {
        return new NotificationPayload(
                UUID.randomUUID().toString(),
                event.orderId(),
                "client",
                "customer-1",
                "sms",
                event.eventType() + " update for order " + event.orderId(),
                Instant.now()
        );
    }
}
