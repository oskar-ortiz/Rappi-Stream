package com.rappistream.smsworker.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class SmsWorkerService {
    private final SmsGateway smsGateway;

    public SmsWorkerService(SmsGateway smsGateway) {
        this.smsGateway = smsGateway;
    }

    @RabbitListener(queues = "sms_queue")
    public void receive(NotificationPayload payload) {
        smsGateway.send(payload);
    }
}
