package com.rappistream.orderprocessor.infrastructure.amqp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.orderprocessor.application.OrderProcessingService;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
// Adaptador de mensajeria registrado como componente de Spring.
@Component
public class OrderCommandRabbitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCommandRabbitListener.class);
    private final ObjectMapper objectMapper;
    private final OrderProcessingService processingService;
    public OrderCommandRabbitListener(ObjectMapper objectMapper, OrderProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }
    @RabbitListener(queues = "${app.rabbitmq.queue:new_orders_queue}")
    public void onMessage(Message message, Channel channel) throws Exception {
        // Identifica esta entrega dentro del canal; no es el ID del pedido.
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // Reconstruye el comando a partir del JSON y delega las reglas al servicio.
            OrderCreatedCommand command = objectMapper.readValue(message.getBody(), OrderCreatedCommand.class);
            processingService.process(command);
            // Confirma solo despues de process(); false significa solo esta entrega.
            channel.basicAck(deliveryTag, false);
        }
        catch (JsonProcessingException | IllegalArgumentException ex) {
            LOGGER.error("Invalid order command rejected: {}", ex.getMessage());
            // Mensaje invalido: no reencolar. Sin dead-letter configurado, se descarta.
            channel.basicReject(deliveryTag, false);
        }
        catch (Exception ex) {
            LOGGER.error("Transient failure processing order command: {}", ex.getMessage());
            // Fallo potencialmente transitorio: reencola esta entrega.
            // Este bloque no impone una espera ni un limite de reintentos.
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
