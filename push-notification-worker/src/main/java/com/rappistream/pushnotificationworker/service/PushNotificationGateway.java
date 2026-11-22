package com.rappistream.pushnotificationworker.service;

public interface PushNotificationGateway {
    void send(NotificationPayload payload);
}
