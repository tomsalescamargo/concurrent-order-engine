package com.swishsales.concurrent.entity;

import java.util.Objects;
import java.util.UUID;

public class Order {

    private final UUID id;
    private final Item item;
    private final Customer customer;
    private OrderStatus orderStatus;

    public Order(UUID id, Item item, Customer customer) {
        this.id = id;
        this.item = item;
        this.customer = customer;
        this.orderStatus = OrderStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
