package com.rappistream.dashboardbackend.infrastructure.websocket;

import com.rappistream.dashboardbackend.application.OrderSnapshotService;
import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import com.rappistream.dashboardbackend.ports.DashboardClientsPort;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {
    private final DashboardClientsPort clients;
    private final OrderSnapshotService snapshotService;

    public DashboardWebSocketHandler(
            DashboardClientsPort clients,
            OrderSnapshotService snapshotService) {
        this.clients = clients;
        this.snapshotService = snapshotService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        clients.connect(session);
        snapshotService.getSnapshot().forEach(update -> clients.sendTo(session, update));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        clients.disconnect(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        clients.disconnect(session);
    }
}
