package com.rappistream.dashboardaggregator.infrastructure.history;

import com.rappistream.dashboardaggregator.domain.EventType;
import com.rappistream.dashboardaggregator.domain.OrderEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaOrderHistoryAdapterTest {

    @Mock
    private OrderHistoryJpaRepository repository;

    @Test
    void recordsEvent() {
        // Arrange
        JpaOrderHistoryAdapter adapter = new JpaOrderHistoryAdapter(repository);
        OrderEvent event = new OrderEvent(EventType.DRIVER_ASSIGNED, "o1", "driver-1", Instant.now());
        // Act
        adapter.record(event);
        // Assert
        verify(repository).save(org.mockito.ArgumentMatchers.any(OrderHistoryJpaEntity.class));
    }
}
