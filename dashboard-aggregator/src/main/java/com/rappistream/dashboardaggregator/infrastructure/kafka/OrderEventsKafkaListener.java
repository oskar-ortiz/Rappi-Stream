package com.rappistream.dashboardaggregator.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.dashboardaggregator.application.OrderStateStore;
import com.rappistream.dashboardaggregator.domain.InvalidOrderTransitionException;
import com.rappistream.dashboardaggregator.domain.OrderEvent;
import com.rappistream.dashboardaggregator.ports.OrderEventsConsumerPort;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class OrderEventsKafkaListener implements ConsumerSeekAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventsKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final OrderEventsConsumerPort eventConsumer;
    private final OrderStateStore stateStore;

    public OrderEventsKafkaListener(
            ObjectMapper objectMapper,
            OrderEventsConsumerPort eventConsumer,
            OrderStateStore stateStore) {
        this.objectMapper = objectMapper;
        this.eventConsumer = eventConsumer;
        this.stateStore = stateStore;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload, @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        final OrderEvent event;
        try {
            event = objectMapper.readValue(payload, OrderEvent.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            LOGGER.warn("Ignoring malformed order event: {}", exception.getMessage());
            return;
        }
        if (!event.orderId().equals(messageKey)) {
            LOGGER.warn("Ignoring order event whose key does not match order_id: key={}, orderId={}",
                    messageKey, event.orderId());
            return;
        }
        try {
            eventConsumer.consume(event);
        } catch (InvalidOrderTransitionException exception) {
            LOGGER.warn("Ignoring invalid order transition for {}: {}", event.orderId(), exception.getMessage());
        }
    }

    @Override
    public void onPartitionsAssigned(
            Map<TopicPartition, Long> assignments,
            ConsumerSeekCallback callback) {
        Collection<TopicPartition> partitions = assignments.keySet();
        stateStore.clear();
        callback.seekToBeginning(partitions);
        LOGGER.info("Rebuilding order views from the beginning of {} assigned Kafka partitions", partitions.size());
    }
}
