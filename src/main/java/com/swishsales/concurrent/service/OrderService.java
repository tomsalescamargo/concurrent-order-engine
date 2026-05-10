package com.swishsales.concurrent.service;

import com.swishsales.concurrent.entity.Customer;
import com.swishsales.concurrent.entity.Item;
import com.swishsales.concurrent.entity.Order;
import com.swishsales.concurrent.entity.OrderStatus;
import com.swishsales.concurrent.pool.CustomThreadPool;
import com.swishsales.concurrent.repository.CustomerRepository;
import com.swishsales.concurrent.repository.ItemRepository;
import com.swishsales.concurrent.future.CustomFuture;

public class OrderService {

    private final Double errorRate;
    private final ItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final CustomThreadPool validationPool;

    public OrderService(Double errorRate, ItemRepository itemRepository, CustomerRepository customerRepository, CustomThreadPool validationPool) {
        this.errorRate = errorRate;
        this.itemRepository = itemRepository;
        this.customerRepository = customerRepository;
        this.validationPool = validationPool;
    }

    public boolean validateOrder(Order order) {

        // 1. Criamos os "recibos" vazios
        CustomFuture<Boolean> orderDataValidationFuture = new CustomFuture<>();
        CustomFuture<Boolean> orderPaymentValidationFuture = new CustomFuture<>();

        // 2. Disparamos a validação de dados na ThreadPool de validation
        try {
            // Validação de dados
            validationPool.submit(() -> {
                try {
                    orderDataValidationFuture.complete(validateOrderData(order));
                } catch (Exception e) {
                    orderDataValidationFuture.completeExceptionally(e);
                }
            });

            // validação de pagamento
            validationPool.submit(() -> {
                try {
                    orderPaymentValidationFuture.complete(validateOrderPayment(order));
                } catch (Exception e) {
                    orderPaymentValidationFuture.completeExceptionally(e);
                }
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            order.setOrderStatus(OrderStatus.FAILED_VALIDATION);
            return false;
        }

        Boolean isOrderDataValid;
        Boolean isOrderPaymentValid;

        try {
            // 3. A Thread do OrderConsumer vai pausar aqui (no .get()) até que
            // as Threads acima chamem o .complete() ou .completeExceptionally()

            // MECANISMO DE TIMEOUT:
            // A inclusão do tempo limite (5 segundos) previne que a thread consumidora
            // sofra de 'starvation' (inanição) ou fique presa num 'deadlock'. Se a API de pagamento externa
            // travar e nunca responder, o sistema não congela para sempre; ele lança uma exceção e segue a vida.
            isOrderDataValid = orderDataValidationFuture.get(5000);
            isOrderPaymentValid = orderPaymentValidationFuture.get(5000);

        } catch (InterruptedException e) {
            // TRATAMENTO DE INTERRUPÇÃO:
            // Se a Thread do Consumer for interrompida pelo sistema (ex: encerramento da aplicação),
            // a flag de interrupção é restaurada e o pedido é marcado como falho de forma segura.
            Thread.currentThread().interrupt();
            order.setOrderStatus(OrderStatus.FAILED_VALIDATION);
            return false;

        } catch (Exception e) {
            // TRATAMENTO DO TIMEOUT OU EXCEÇÕES DO FUTURE:
            // Cai neste bloco se o get(5000) estourar o tempo limite (lançando RuntimeException)
            // ou se a validação dentro da thread disparar um completeExceptionally().
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
            Thread.sleep(1000); // Simula API externa
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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