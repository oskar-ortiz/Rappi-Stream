package com.rappistream.dashboardbackend.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.dashboardbackend.application.ApplyOrderStatusUpdateUseCase;
import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OrderIngestWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderIngestWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final ApplyOrderStatusUpdateUseCase applyUpdate;

    public OrderIngestWebSocketHandler(
            ObjectMapper objectMapper,
            ApplyOrderStatusUpdateUseCase applyUpdate) {
        this.objectMapper = objectMapper;
        this.applyUpdate = applyUpdate;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            applyUpdate.apply(objectMapper.readValue(message.getPayload(), OrderStatusUpdate.class));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            LOGGER.warn("Ignoring invalid dashboard ingest message: {}", exception.getMessage());
        }
    }
}
