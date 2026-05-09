package com.swishsales.concurrent;

import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;
import com.swishsales.concurrent.task.OrderCreator;

import javax.swing.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {

        // ImplementMe - Producer-consumer
        BlockingQueue<Order> ordersQueue = new LinkedBlockingQueue<>();

        CustomerRepository customerRepository = new CustomerRepository();
        ItemRepository itemRepository = new ItemRepository();

        String errorPercentageInput = JOptionPane.showInputDialog(
                "Digite a taxa de erro desejada em porcentagem (ex: 15): "
        );
        Double errorRate = Double.parseDouble(errorPercentageInput) / 100.0;

        // ImplementME - Thread Pool
        ExecutorService ordersProducerPool = Executors.newFixedThreadPool(7);
        int numberOfOrders = 10;
        for (int i = 0; i < numberOfOrders; i++) {
            ordersProducerPool.execute(new OrderCreator(customerRepository, itemRepository, ordersQueue));
        }
    }
}