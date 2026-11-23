package com.rappistream.orderprocessor.infrastructure.kafka;
import com.rappistream.orderprocessor.messaging.OrderEvent;
import com.rappistream.orderprocessor.ports.OrderEventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
// Adaptador de mensajeria registrado como componente de Spring.
@Component
public class KafkaOrderEventPublisherAdapter implements OrderEventPublisherPort {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;
    public KafkaOrderEventPublisherAdapter(KafkaTemplate<String, OrderEvent> kafkaTemplate, @org.springframework.beans.factory.annotation.Value("${app.kafka.topic:order_events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    @Override
    public void publish(OrderEvent event) {
        try {
            // Argumentos: topic, clave orderId y contenido del evento.
            // Con particiones estables, la misma clave conserva la particion y su orden.
            // get espera hasta 10 segundos por la publicacion, no por los consumidores.
            kafkaTemplate.send(topic, event.orderId(), event).get(10, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            // Propaga el fallo hasta el listener RabbitMQ, que puede reencolar el comando.
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}
