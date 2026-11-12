package com.rappistream.orderapi.infrastructure.config;

import com.rappistream.orderapi.infrastructure.amqp.RabbitOrderCommandAdapter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue newOrdersQueue(@Value("${app.rabbitmq.queue:new_orders_queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        @Value("${app.broker.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            // confirmation handled via CorrelationData future
        });
        return template;
    }
}
