package com.rappi.stream.application;

import com.rappi.stream.domain.DriverAssignedEvent;

public interface OrderEventPublisherPort {
    void publicarDriverAssigned(DriverAssignedEvent event);

    // Sobrecarga para compatibilidad con formato de mensaje
    default void publicarDriverAssigned(String orderId, String driverId) {
        publicarDriverAssigned(new DriverAssignedEvent(orderId, driverId, java.time.Instant.now().toString()));
    }
}