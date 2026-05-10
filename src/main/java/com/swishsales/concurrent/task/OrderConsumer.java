package com.swishsales.concurrent.task;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.entity.OrderStatus;
import com.swishsales.concurrent.service.LogisticsService;
import com.swishsales.concurrent.service.OrderService;


/**
 * Runnable long-lived: cada instância roda dentro de um worker do consumer pool e fica em
 * loop consumindo Orders da orderQueue. Diferente do OrderProducer (one-shot), uma única
 * instância processa N pedidos durante toda a vida do programa.
 *
 * O loop só termina ao receber Order.POISON_ORDER, sentinela injetada pelo Main durante
 * o shutdown coordenado.
 */
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
                // Detecção da sentinela de fim ANTES de qualquer acesso aos campos do Order.
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
