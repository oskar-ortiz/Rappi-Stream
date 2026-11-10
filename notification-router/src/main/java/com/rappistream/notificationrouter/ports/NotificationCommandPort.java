package com.rappistream.notificationrouter.ports;

import com.rappistream.notificationrouter.domain.NotificationPayload;
import com.rappistream.notificationrouter.service.NotificationRouterService.OrderEvent;

public interface NotificationCommandPort {
    boolean supports(String eventType);

    String routingKey();

    NotificationPayload toPayload(OrderEvent event);
}
