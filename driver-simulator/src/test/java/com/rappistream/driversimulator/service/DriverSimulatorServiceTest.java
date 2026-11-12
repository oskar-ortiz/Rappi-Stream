package com.rappistream.driversimulator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DriverSimulatorServiceTest {
    @Mock
    private KafkaTemplate<String, DriverSimulatorService.DriverEvent> kafkaTemplate;

    @Mock
    private ScheduledExecutorService driverTripScheduler;

    private DriverSimulatorService service;

    @BeforeEach
    void setUp() {
        service = new DriverSimulatorService(kafkaTemplate, driverTripScheduler, "configured-order-events", 5, 10);
    }

    @Test
    void shouldScheduleAndPublishTripEventsUsingOrderIdAsKey() {
        DriverSimulatorService.DriverEvent assignment =
                new DriverSimulatorService.DriverEvent("DRIVER_ASSIGNED", "order-1", "driver-7", Instant.now());
        service.listen(new org.apache.kafka.clients.consumer.ConsumerRecord<>(
                "order_events", 0, 0L, "order-1", assignment));

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(driverTripScheduler, times(2)).schedule(tasks.capture(), delays.capture(), eq(TimeUnit.SECONDS));
        assertEquals(java.util.List.of(5L, 10L), delays.getAllValues());

        tasks.getAllValues().forEach(Runnable::run);

        ArgumentCaptor<DriverSimulatorService.DriverEvent> events =
                ArgumentCaptor.forClass(DriverSimulatorService.DriverEvent.class);
        verify(kafkaTemplate, times(2)).send(eq("configured-order-events"), eq("order-1"), events.capture());
        assertEquals("DRIVER_ARRIVING", events.getAllValues().get(0).eventType());
        assertEquals("ORDER_DELIVERED", events.getAllValues().get(1).eventType());
        assertEquals("driver-7", events.getAllValues().get(0).driverId());
    }

    @Test
    void shouldIgnoreEventsThatAreNotDriverAssigned() {
        DriverSimulatorService.DriverEvent arriving =
                new DriverSimulatorService.DriverEvent("DRIVER_ARRIVING", "order-1", "driver-7", Instant.now());

        service.listen(new org.apache.kafka.clients.consumer.ConsumerRecord<>(
                "configured-order-events", 0, 0L, "order-1", arriving));

        verifyNoInteractions(driverTripScheduler, kafkaTemplate);
    }
}
