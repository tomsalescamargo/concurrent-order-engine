package com.swishsales.concurrent.task;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.entity.OrderStatus;
import com.swishsales.concurrent.service.LogisticsService;
import com.swishsales.concurrent.service.OrderService;


public class OrderConsumer implements Runnable {

    private final CustomBlockingQueue<Order> ordersQueue;
    private final OrderService orderService;
    private final LogisticsService logisticsService;

    public OrderConsumer(CustomBlockingQueue<Order> ordersQueue, OrderService orderService, LogisticsService logisticsService) {
        this.ordersQueue = ordersQueue;
        this.orderService = orderService;
        this.logisticsService = logisticsService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Thread sleeps if the queue is empty
                Order order = ordersQueue.take();
                if (order == Order.POISON_ORDER) {
                    return;
                }
                order.setOrderStatus(OrderStatus.PROCESSING);
                System.out.println("Iniciando processamento do pedido: " + order.getId());

                // Delegates validation to the service layer
                boolean isOrderValid = orderService.validateOrder(order);

                if (isOrderValid) {
                    logisticsService.processDelivery(order);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
