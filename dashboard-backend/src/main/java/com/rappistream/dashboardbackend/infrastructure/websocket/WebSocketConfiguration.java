package com.rappistream.dashboardbackend.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final OrderIngestWebSocketHandler ingestHandler;
    private final DashboardWebSocketHandler dashboardHandler;

    public WebSocketConfiguration(
            OrderIngestWebSocketHandler ingestHandler,
            DashboardWebSocketHandler dashboardHandler) {
        this.ingestHandler = ingestHandler;
        this.dashboardHandler = dashboardHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ingestHandler, "/ws/ingest").setAllowedOriginPatterns("*");
        registry.addHandler(dashboardHandler, "/ws/dashboard").setAllowedOriginPatterns("*");
    }
}
