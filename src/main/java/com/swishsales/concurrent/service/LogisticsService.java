package com.swishsales.concurrent.service;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.entity.OrderStatus;

public class LogisticsService {

    private final Double errorRate;

    public LogisticsService(Double errorRate) {
        this.errorRate = errorRate;
    }

    public void processDelivery(Order order) {
        System.out.println("Logística iniciada para o pedido: " + order.getId());
        order.setOrderStatus(OrderStatus.SHIPPED);

        try {
            // Simulates physical delivery shipping
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Transporte interrompido para o pedido: " + order.getId());
            order.setOrderStatus(OrderStatus.FAILED_LOGISTICS);
            return;
        }

        // Checks for delivery damage based on the error rate
        if (shouldFail()) {
            System.out.println("Dano na entrega! Pedido " + order.getId() + " falhou na logística.");
            order.setOrderStatus(OrderStatus.FAILED_LOGISTICS);
        } else {
            System.out.println("Sucesso! Pedido " + order.getId() + " entregue.");
            order.setOrderStatus(OrderStatus.DELIVERED);
        }
    }

    private boolean shouldFail() {
        return Math.random() < this.errorRate;
    }
}