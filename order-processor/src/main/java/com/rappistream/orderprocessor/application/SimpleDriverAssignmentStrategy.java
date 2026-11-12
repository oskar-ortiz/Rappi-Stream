package com.rappistream.orderprocessor.application;

import com.rappistream.orderprocessor.domain.DriverAssignmentStrategy;
import com.rappistream.orderprocessor.domain.Order;

import java.util.UUID;

public class SimpleDriverAssignmentStrategy implements DriverAssignmentStrategy {

    @Override
    public String assignDriver(Order order) {
        String driverId = "driver-" + UUID.randomUUID();
        order.assignDriver(driverId);
        return driverId;
    }
}
