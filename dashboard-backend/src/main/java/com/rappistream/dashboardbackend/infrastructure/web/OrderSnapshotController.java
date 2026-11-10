package com.rappistream.dashboardbackend.infrastructure.web;

import com.rappistream.dashboardbackend.application.OrderSnapshotService;
import com.rappistream.dashboardbackend.domain.OrderStatusUpdate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderSnapshotController {
    private final OrderSnapshotService snapshotService;

    public OrderSnapshotController(OrderSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/api/orders")
    public List<OrderStatusUpdate> getOrders() {
        return snapshotService.getSnapshot();
    }
}
