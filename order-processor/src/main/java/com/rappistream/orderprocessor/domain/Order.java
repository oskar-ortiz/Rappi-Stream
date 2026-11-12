package com.rappistream.orderprocessor.domain;

import java.util.List;
import java.util.Objects;

public final class Order {
    private final String orderId;
    private final String customerId;
    private final String customerPhone;
    private final String address;
    private final List<OrderItem> items;
    private final java.time.Instant createdAt;
    private OrderState state;
    private String driverId;

    private Order(String orderId, String customerId, String customerPhone, String address,
                  List<OrderItem> items, java.time.Instant createdAt, OrderState state, String driverId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.customerPhone = Objects.requireNonNull(customerPhone, "customerPhone");
        this.address = Objects.requireNonNull(address, "address");
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.state = Objects.requireNonNull(state, "state");
        this.driverId = driverId;
    }

    public static Order create(String orderId, String customerId, String customerPhone, String address,
                               List<OrderItem> items, java.time.Instant createdAt) {
        return new Order(orderId, customerId, customerPhone, address, items, createdAt, OrderState.CREATED, null);
    }

    public void assignDriver(String driverId) {
        this.state = this.state.assignDriver();
        this.driverId = Objects.requireNonNull(driverId, "driverId");
    }

    public String orderId() { return orderId; }
    public String customerId() { return customerId; }
    public String customerPhone() { return customerPhone; }
    public String address() { return address; }
    public List<OrderItem> items() { return items; }
    public java.time.Instant createdAt() { return createdAt; }
    public OrderState state() { return state; }
    public String driverId() { return driverId; }
}
