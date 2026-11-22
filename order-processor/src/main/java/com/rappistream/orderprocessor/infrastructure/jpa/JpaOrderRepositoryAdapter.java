package com.rappistream.orderprocessor.infrastructure.jpa;

import com.rappistream.orderprocessor.domain.Order;
import com.rappistream.orderprocessor.domain.OrderItem;
import com.rappistream.orderprocessor.ports.OrderRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository repository;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(String orderId) {
        return repository.findById(orderId).map(this::toDomain);
    }

    private OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setOrderId(order.orderId());
        entity.setCustomerId(order.customerId());
        entity.setCustomerPhone(order.customerPhone());
        entity.setAddress(order.address());
        List<OrderItemEmbeddable> items = new ArrayList<>();
        for (OrderItem item : order.items()) {
            items.add(new OrderItemEmbeddable(item.sku(), item.quantity()));
        }
        entity.setItems(items);
        entity.setState(order.state().name());
        entity.setDriverId(order.driverId());
        entity.setCreatedAt(order.createdAt());
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemEmbeddable item : entity.getItems()) {
            items.add(new OrderItem(item.getSku(), item.getQuantity()));
        }
        Order order = Order.create(
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getCustomerPhone(),
                entity.getAddress(),
                items,
                entity.getCreatedAt());
        if ("DRIVER_ASSIGNED".equals(entity.getState()) && entity.getDriverId() != null) {
            order.assignDriver(entity.getDriverId());
        }
        return order;
    }
}
