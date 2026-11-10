package com.rappistream.pushnotificationworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushNotificationGateway implements PushNotificationGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPushNotificationGateway.class);

    @Override
    public void send(NotificationPayload payload) {
        LOGGER.info("Push notification simulated for recipient {}: {}", payload.recipientId(), payload.message());
    }
}
