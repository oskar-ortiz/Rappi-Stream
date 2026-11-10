package com.rappistream.dashboardaggregator.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappistream.dashboardaggregator.application.OrderStateStore;
import com.rappistream.dashboardaggregator.domain.OrderViewSnapshot;
import com.rappistream.dashboardaggregator.ports.DashboardPublisherPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DashboardWebSocketPublisher implements DashboardPublisherPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardWebSocketPublisher.class);
    private static final long MAX_BACKOFF_SECONDS = 30;

    private final ObjectMapper objectMapper;
    private final OrderStateStore stateStore;
    private final URI ingestUri;
    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "dashboard-websocket-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<WebSocketSession> session = new AtomicReference<>();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicInteger failedAttempts = new AtomicInteger();

    public DashboardWebSocketPublisher(
            ObjectMapper objectMapper,
            OrderStateStore stateStore,
            @Value("${app.dashboard.ingest-url}") String ingestUrl) {
        this.objectMapper = objectMapper;
        this.stateStore = stateStore;
        this.ingestUri = URI.create(ingestUrl);
    }

    @PostConstruct
    void connectOnStartup() {
        connect();
    }

    @Override
    public void publish(OrderViewSnapshot snapshot) {
        send(snapshot);
    }

    @Override
    public void publishSnapshot(Collection<OrderViewSnapshot> snapshots) {
        snapshots.forEach(this::send);
    }

    @PreDestroy
    void stop() {
        active.set(false);
        reconnectExecutor.shutdownNow();
        WebSocketSession current = session.getAndSet(null);
        if (current != null && current.isOpen()) {
            try {
                current.close(CloseStatus.NORMAL);
            } catch (IOException exception) {
                LOGGER.warn("Could not close dashboard WebSocket cleanly", exception);
            }
        }
    }

    private void connect() {
        if (!active.get() || !connecting.compareAndSet(false, true)) {
            return;
        }
        client.execute(new IngestSessionHandler(), ingestUri.toString())
                .whenComplete((connected, failure) -> {
                    connecting.set(false);
                    if (failure != null) {
                        LOGGER.warn("Dashboard WebSocket connection failed: {}", failure.getMessage());
                        scheduleReconnect();
                    }
                });
    }

    private void send(OrderViewSnapshot snapshot) {
        WebSocketSession current = session.get();
        if (current == null || !current.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            synchronized (current) {
                if (current.isOpen()) {
                    current.sendMessage(new TextMessage(json));
                }
            }
        } catch (JsonProcessingException exception) {
            LOGGER.error("Could not serialize dashboard update for order {}", snapshot.orderId(), exception);
        } catch (IOException exception) {
            LOGGER.warn("Dashboard WebSocket send failed: {}", exception.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!active.get() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        int attempt = Math.min(failedAttempts.getAndIncrement(), 5);
        long delay = Math.min(1L << attempt, MAX_BACKOFF_SECONDS);
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, delay, TimeUnit.SECONDS);
    }

    private final class IngestSessionHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession connected) {
            session.set(connected);
            failedAttempts.set(0);
            LOGGER.info("Connected to dashboard backend at {}", ingestUri);
            publishSnapshot(stateStore.snapshot());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession closed, CloseStatus status) {
            session.compareAndSet(closed, null);
            LOGGER.warn("Dashboard WebSocket closed with status {}", status);
            scheduleReconnect();
        }

        @Override
        public void handleTransportError(WebSocketSession failedSession, Throwable exception) {
            session.compareAndSet(failedSession, null);
            LOGGER.warn("Dashboard WebSocket transport error: {}", exception.getMessage());
            scheduleReconnect();
        }
    }
}
