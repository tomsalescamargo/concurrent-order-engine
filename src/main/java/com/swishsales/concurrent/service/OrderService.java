package com.swishsales.concurrent.service;

import com.swishsales.concurrent.entity.Order;

public class OrderService {

    private Double errorRate;

    public OrderService(Double errorRate) {
        this.errorRate = errorRate;
    }

    public boolean validateOrder(Order order) {
        return true;
    }
}
