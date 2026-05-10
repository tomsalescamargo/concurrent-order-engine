package com.swishsales.concurrent.service;

import com.swishsales.concurrent.entity.Customer;
import com.swishsales.concurrent.entity.Item;
import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.entity.OrderStatus;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class OrderService {

    private final Double errorRate;
    private final ItemRepository itemRepository;
    private final CustomerRepository customerRepository;

    public OrderService(Double errorRate, ItemRepository itemRepository, CustomerRepository customerRepository) {
        this.errorRate = errorRate;
        this.itemRepository = itemRepository;
        this.customerRepository = customerRepository;
    }

    public boolean validateOrder(Order order) {
        // ImplementME - Future
        // Validates if the Customer and Item related to Order are valid
        Future<Boolean> orderDataValidationFuture = CompletableFuture.supplyAsync(
                () -> validateOrderData(order)
        );

        // Validates the order payment (simulates a fake external API call)
        Future<Boolean> orderPaymentValidationFuture = CompletableFuture.supplyAsync(
                () -> validateOrderPayment(order)
        );

        Boolean isOrderDataValid;
        Boolean isOrderPaymentValid;
        try {
            isOrderDataValid = orderDataValidationFuture.get();
            isOrderPaymentValid = orderPaymentValidationFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            order.setOrderStatus(OrderStatus.FAILED_VALIDATION);
            return false;
        }

        if (!isOrderDataValid) {
            order.setOrderStatus(OrderStatus.FAILED_VALIDATION);
            return false;
        }

        if (!isOrderPaymentValid) {
            order.setOrderStatus(OrderStatus.FAILED_FINANCIAL);
            return false;
        }

        return true;
    }

    private Boolean validateOrderData(Order order) {
        String itemId = order.getItem().getId();
        Item item = itemRepository.findById(itemId);

        String customerId = order.getCustomer().getId();
        Customer customer = customerRepository.findById(customerId);

        if (item == null || customer == null) {
            System.out.println("Dados do pedido " + order.getId() + " inválidos");
            return false;
        }

        if (shouldFail()) {
            System.out.println("Validação dos dados do pedido " + order.getId() + " falhou");
            return false;
        }

        return true;
    }

    private Boolean validateOrderPayment(Order order) {
        try {
            Thread.sleep(1000); // Simulates external payment API
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (shouldFail()) {
            System.out.println("Não foi possível processar o pagamento do pedido " + order.getId());
            return false;
        }

        return true;
    }

    private boolean shouldFail() {
        return Math.random() < this.errorRate;
    }
}
