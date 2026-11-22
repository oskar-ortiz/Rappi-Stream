package com.rappistream.dashboardbackend.ports;

import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import org.springframework.web.socket.WebSocketSession;

public interface DashboardClientsPort {
    void connect(WebSocketSession session);

    void disconnect(WebSocketSession session);

    void sendTo(WebSocketSession session, OrderStatusUpdate update);

    void broadcast(OrderStatusUpdate update);
}
