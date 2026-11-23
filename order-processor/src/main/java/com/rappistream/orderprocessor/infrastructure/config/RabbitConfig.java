package com.rappistream.orderprocessor.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue newOrdersQueue(@Value("${app.rabbitmq.queue:new_orders_queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    // Declarar el DLX y su cola no conecta automaticamente la cola original con ellos.
    // new_orders_queue necesita argumentos o una politica de dead-letter para enviar rechazos aqui.
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("new_orders_dlx");
    }

    @Bean
    public Queue deadLetterQueue(@Value("${app.rabbitmq.queue:new_orders_queue}") String queueName) {
        return QueueBuilder.durable(queueName + "_dlq").build();
    }

    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                                     DirectExchange deadLetterExchange,
                                     @Value("${app.rabbitmq.queue:new_orders_queue}") String queueName) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(queueName);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // El listener decide explicitamente cuando confirmar, rechazar o reencolar.
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        // Politica del contenedor; basicNack(..., true) solicita reencolar explicitamente.
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
