package com.rappistream.smsworker.service;

public interface SmsGateway {
    void send(NotificationPayload payload);
}
