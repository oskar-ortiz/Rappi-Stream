package com.rappistream.notificationrouter.strategy;

import com.rappistream.notificationrouter.domain.NotificationPayload;
import com.rappistream.notificationrouter.ports.NotificationCommandPort;
import com.rappistream.notificationrouter.service.NotificationRouterService.OrderEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DriverPushNotificationCommand implements NotificationCommandPort {
    @Override
    public boolean supports(String eventType) {
        return "DRIVER_ASSIGNED".equals(eventType);
    }

    @Override
    public String routingKey() {
        return "notify.driver.push";
    }

    @Override
    public NotificationPayload toPayload(OrderEvent event) {
        return new NotificationPayload(
                UUID.randomUUID().toString(),
                event.orderId(),
                "driver",
                event.driverId(),
                "push",
                "Driver assigned for order " + event.orderId(),
                Instant.now()
        );
    }
}
