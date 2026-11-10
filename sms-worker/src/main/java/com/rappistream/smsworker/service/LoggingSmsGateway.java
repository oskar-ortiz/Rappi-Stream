package com.rappistream.smsworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsGateway implements SmsGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public void send(NotificationPayload payload) {
        LOGGER.info("SMS simulated for recipient {}: {}", payload.recipientId(), payload.message());
    }
}
