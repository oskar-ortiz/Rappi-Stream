package com.rappistream.orderapi.infrastructure.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.orderapi.application.BrokerUnavailableException;
import com.rappistream.orderapi.domain.OrderCreatedCommand;
import com.rappistream.orderapi.ports.NewOrderCommandPort;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// Adaptador de salida: implementa el puerto del negocio usando RabbitMQ.
// Spring inyecta por constructor el cliente RabbitTemplate y el serializador ObjectMapper.
@Component
public class RabbitOrderCommandAdapter implements NewOrderCommandPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final long confirmTimeoutMs;

    public RabbitOrderCommandAdapter(RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${app.rabbitmq.queue:new_orders_queue}") String queueName,
                                    @Value("${app.broker.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Override
    public void publish(OrderCreatedCommand command) {
        final String json;
        try {
            // Convierte el objeto Java a JSON para comunicar procesos con memorias independientes.
            json = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize order command", e);
        }
        // Asocia la publicacion con su confirmacion; no elimina duplicados.
        org.springframework.amqp.rabbit.connection.CorrelationData correlationData =
                new org.springframework.amqp.rabbit.connection.CorrelationData(command.orderId());
        // Cuerpo en bytes UTF-8 y tipo JSON. PERSISTENT se complementa con una cola durable.
        // messageId identifica el pedido; RabbitMQ no deduplica automaticamente por este valor.
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(command.orderId())
                .build();
        try {
            // Exchange vacio: exchange predeterminado. La routing key es el nombre de la cola.
            rabbitTemplate.send("", queueName, message, correlationData);
            // Espera hasta 5000 ms por defecto. Confirma publicacion, no procesamiento del consumidor.
            // Un timeout no demuestra que el mensaje nunca llego al broker.
            org.springframework.amqp.rabbit.connection.CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            // Aqui se revisa el ACK, pero no un posible mensaje devuelto por falta de ruta.
            if (confirm == null || !confirm.isAck()) {
                throw new BrokerUnavailableException("Broker did not confirm publication");
            }
        } catch (BrokerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new BrokerUnavailableException("Broker unavailable or publication not confirmed", e);
        }
    }
}
