package com.rappistream.notificationrouter.strategy;

import com.rappistream.notificationrouter.ports.NotificationCommandPort;
import com.rappistream.notificationrouter.service.NotificationRouterService.OrderEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationCommandResolver {
    private final List<NotificationCommandPort> commands;

    public NotificationCommandResolver(List<NotificationCommandPort> commands) {
        this.commands = commands;
    }

    public NotificationCommandPort resolve(OrderEvent event) {
        return commands.stream()
                .filter(command -> command.supports(event.eventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No notification strategy for event type: " + event.eventType()));
    }
}
