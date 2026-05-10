package com.swishsales.concurrent;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.pool.CustomThreadPool;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;
import com.swishsales.concurrent.service.LogisticsService;
import com.swishsales.concurrent.service.OrderService;
import com.swishsales.concurrent.task.CustomBlockingQueue;
import com.swishsales.concurrent.task.OrderConsumer;
import com.swishsales.concurrent.task.OrderProducer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // CONFIG
        // tamanho da fila produtor consumidor
        int orderQueueSize = 100;

        // tamanho da fila dos Thread Pools prod-cons
        int taskQueueSize = 100;

        // tamanho da fila do Thread Pool de validação
        int validationQueueSize = 2 * taskQueueSize;

        // numero de threads das pools
        int numberOfProducerThreads= 2;
        int numberOfConsumerThreads= 10;
        int numberOfValidationPoolThreads = 2 * numberOfConsumerThreads;

        // numero de pedidos a serem simulados
        int numberOfOrders = 100;

        // Producer-consumer
        CustomBlockingQueue<Order> orderQueue = new CustomBlockingQueue<>(orderQueueSize);
        // Repository init
        CustomerRepository customerRepository = new CustomerRepository();
        ItemRepository itemRepository = new ItemRepository();

        // Service init
        Double errorRate = getErrorRate();

        // Initialize validation pool
        CustomThreadPool validationPool = new CustomThreadPool(numberOfValidationPoolThreads, validationQueueSize);
        OrderService orderService = new OrderService(
                errorRate,
                itemRepository,
                customerRepository,
                validationPool
        );
        LogisticsService logisticsService = new LogisticsService(errorRate);


        // Producer threads, task OrderProducer creates an order and puts into the orders queue
        CustomThreadPool ordersProducerPool = new CustomThreadPool(numberOfProducerThreads, taskQueueSize);
        for (int i = 0; i < numberOfOrders; i++) {
            ordersProducerPool.submit(
                    new OrderProducer(customerRepository, itemRepository, orderQueue)
            );
        }

        // Consumer threads, task OrderConsumer waits for the
        CustomThreadPool ordersConsumerPool = new CustomThreadPool(numberOfConsumerThreads, taskQueueSize);
        for (int i = 0; i < numberOfConsumerThreads; i++) {
            ordersConsumerPool.submit(
                    new OrderConsumer(orderQueue, orderService, logisticsService)
            );
        }

        // Desliga producer pool
        ordersProducerPool.shutdown();
        ordersProducerPool.awaitTermination();

        // Importante: Injeta poison orders pra acordar as threads consumidoras
        // que estão dormindo para elas finalizarem
        for (int i = 0; i < numberOfConsumerThreads; i++) {
            orderQueue.put(Order.POISON_ORDER);
        }
        ordersConsumerPool.shutdown();
        ordersConsumerPool.awaitTermination();

        validationPool.shutdown();
        validationPool.awaitTermination();
    }

    private static Double getErrorRate() {
        String errorPercentageInput = JOptionPane.showInputDialog(
                "Digite a taxa de erro desejada em porcentagem (ex: 15): "
        );

        return Double.parseDouble(errorPercentageInput) / 100.0;
    }
}