package com.swishsales.concurrent;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;
import com.swishsales.concurrent.service.OrderService;
import com.swishsales.concurrent.task.OrderConsumer;
import com.swishsales.concurrent.task.OrderProducer;

import javax.swing.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        // ImplementMe - Producer-consumer
        BlockingQueue<Order> ordersQueue = new LinkedBlockingQueue<>();

        // Repository init
        CustomerRepository customerRepository = new CustomerRepository();
        ItemRepository itemRepository = new ItemRepository();

        // Service init
        Double errorRate = getErrorRate();
        OrderService orderService = new OrderService(errorRate);


        // ImplementME - Thread Pool
        // Producer threads, task OrderProducer creates an order and puts into the orders queue
        ExecutorService ordersProducerPool = Executors.newFixedThreadPool(7);
        int numberOfOrders = 10;
        for (int i = 0; i < numberOfOrders; i++) {
            ordersProducerPool.execute(
                    new OrderProducer(customerRepository, itemRepository, ordersQueue)
            );
        }

        // Consumer threads, task OrderConsumer waits for the
        //  order and delegates it to the order service validation
        int numberOfConsumers = 5;
        ExecutorService ordersConsumerPool = Executors.newFixedThreadPool(numberOfConsumers);
        for (int i = 0; i < numberOfConsumers; i++) {
            ordersConsumerPool.execute(
                    new OrderConsumer(ordersQueue, orderService)
            );
        }
    }

    private static Double getErrorRate() {
        String errorPercentageInput = JOptionPane.showInputDialog(
                "Digite a taxa de erro desejada em porcentagem (ex: 15): "
        );

        return Double.parseDouble(errorPercentageInput) / 100.0;
    }
}