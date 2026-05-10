package com.swishsales.concurrent.task;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.service.OrderService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderConsumer implements Runnable {

    private BlockingQueue<Order> ordersQueue;
    private OrderService orderService;

    public OrderConsumer(BlockingQueue<Order> ordersQueue, OrderService orderService) {
        this.ordersQueue = ordersQueue;
        this.orderService = orderService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Thread sleeps if the queue is empty
                Order order = ordersQueue.take();

                // Delegates validation to the service layer
                orderService.validateOrder(order);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
