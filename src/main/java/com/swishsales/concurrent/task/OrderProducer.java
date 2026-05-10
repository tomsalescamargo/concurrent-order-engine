package com.swishsales.concurrent.task;

import com.swishsales.concurrent.entity.Customer;
import com.swishsales.concurrent.entity.Item;
import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;

import java.util.UUID;

/**
 * Runnable one-shot: cria exatamente UM pedido e termina.
 */
public class OrderProducer implements Runnable {

    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final CustomBlockingQueue<Order> orderQueue;

    public OrderProducer(
            CustomerRepository customerRepository,
            ItemRepository itemRepository,
            CustomBlockingQueue<Order> orderQueue
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
            System.out.println("Novo pedido gerado na fila: " + order.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
