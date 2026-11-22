package com.rappistream.dashboardbackend.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.dashboardbackend.application.ApplyOrderStatusUpdateUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderIngestWebSocketHandlerTest {
    private final ApplyOrderStatusUpdateUseCase applyUpdate = Mockito.mock(ApplyOrderStatusUpdateUseCase.class);
    private final OrderIngestWebSocketHandler handler =
            new OrderIngestWebSocketHandler(new ObjectMapper().findAndRegisterModules(), applyUpdate);
    private final WebSocketSession session = Mockito.mock(WebSocketSession.class);

    @Test
    void acceptsValidUpdateWithoutSpringContext() {
        handler.handleTextMessage(session, new TextMessage(
                "{\"order_id\":\"order-1\",\"status\":\"EN_CAMINO\","
                        + "\"driver_id\":\"driver-1\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"));

        verify(applyUpdate).apply(Mockito.argThat(update ->
                update.orderId().equals("order-1") && update.status().name().equals("EN_CAMINO")));
    }

    @Test
    void ignoresInvalidMessageWithoutCallingUseCase() {
        handler.handleTextMessage(session, new TextMessage("{invalid"));

        verifyNoInteractions(applyUpdate);
    }
}
