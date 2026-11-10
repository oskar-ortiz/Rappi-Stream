package com.rappistream.dashboardbackend.infrastructure.websocket;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.DashboardClientsPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DashboardClientRegistry implements DashboardClientsPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardClientRegistry.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public DashboardClientRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void connect(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void disconnect(WebSocketSession session) {
        sessions.remove(session);
    }

    @Override
    public void sendTo(WebSocketSession session, OrderStatusUpdate update) {
        if (!session.isOpen()) {
            disconnect(session);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(update);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (JsonProcessingException exception) {
            LOGGER.error("Could not serialize dashboard update for order {}", update.orderId(), exception);
        } catch (IOException exception) {
            LOGGER.warn("Could not send dashboard update to session {}: {}",
                    session.getId(), exception.getMessage());
            disconnect(session);
        }
    }

    @Override
    public void broadcast(OrderStatusUpdate update) {
        sessions.forEach(session -> sendTo(session, update));
    }
}
