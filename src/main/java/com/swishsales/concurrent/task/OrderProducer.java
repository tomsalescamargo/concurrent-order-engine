package com.swishsales.concurrent.task;

import com.swishsales.concurrent.entity.Customer;
import com.swishsales.concurrent.entity.Item;
import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class OrderProducer implements Runnable {

    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final BlockingQueue<Order> orderQueue;

    public OrderProducer(
            CustomerRepository customerRepository,
            ItemRepository itemRepository,
            BlockingQueue<Order> orderQueue
    ) {
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
        this.orderQueue = orderQueue;
    }

    @Override
    public void run() {
        UUID id = UUID.randomUUID();
        Item item = itemRepository.getRandomItem();
        Customer customer = customerRepository.getRandomCustomer();
        Order order = new Order(id, item, customer);

        try {
            orderQueue.put(order);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
