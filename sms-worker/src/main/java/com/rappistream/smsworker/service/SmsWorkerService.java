package com.rappistream.smsworker.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class SmsWorkerService {
    // Interfaz del envio: la implementacion inyectada decide como enviar o simular el SMS.
    private final SmsGateway smsGateway;

    public SmsWorkerService(SmsGateway smsGateway) {
        this.smsGateway = smsGateway;
    }

    // Spring convierte el mensaje al payload. Varios workers se reparten las entregas.
    // No hay ACK manual aqui: el reconocimiento depende del contenedor configurado.
    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void receive(NotificationPayload payload) {
        // Delega el envio; mantiene separado el transporte RabbitMQ del gateway.
        smsGateway.send(payload);
    }
}
