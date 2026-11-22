package com.rappistream.pushnotificationworker.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationWorkerService {
    private final PushNotificationGateway pushNotificationGateway;

    public PushNotificationWorkerService(PushNotificationGateway pushNotificationGateway) {
        this.pushNotificationGateway = pushNotificationGateway;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void receive(NotificationPayload payload) {
        pushNotificationGateway.send(payload);
    }
}
