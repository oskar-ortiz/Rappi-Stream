package com.rappistream.orderprocessor.infrastructure.amqp;

import com.rappistream.orderprocessor.application.OrderProcessingService;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.messaging.OrderCreatedCommand;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandRabbitListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderProcessingService processingService;

    @Mock
    private Channel channel;

    @Test
    void invalidMessageIsRejectedWithoutProcessing() throws Exception {
        // Arrange
        OrderCommandRabbitListener listener = new OrderCommandRabbitListener(objectMapper, processingService);
        Message message = new Message("bad".getBytes(), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(1L);
        when(objectMapper.readValue(any(byte[].class), eq(OrderCreatedCommand.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonGenerationException("bad"));
        // Act
        listener.onMessage(message, channel);
        // Assert
        verify(processingService, never()).process(any());
        verify(channel).basicReject(1L, false);
    }

    @Test
    void serviceFailureDoesNotAck() throws Exception {
        // Arrange
        OrderCommandRabbitListener listener = new OrderCommandRabbitListener(objectMapper, processingService);
        Message message = new Message("{}".getBytes(), new MessageProperties());
        message.getMessageProperties().setDeliveryTag(2L);
        OrderCreatedCommand cmd = new OrderCreatedCommand(
                "o1", "c1", "+1234567890", "addr", List.of(new OrderItem("sku", 1)), java.time.Instant.now());
        when(objectMapper.readValue(any(byte[].class), eq(OrderCreatedCommand.class)))
                .thenReturn(cmd);
        org.mockito.Mockito.doThrow(new RuntimeException("db down")).when(processingService).process(cmd);
        // Act
        listener.onMessage(message, channel);
        // Assert
        verify(channel, never()).basicAck(2L, false);
    }
}
