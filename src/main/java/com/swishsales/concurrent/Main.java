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

/**
 * Ponto de entrada e orquestração da simulação.
 *
 * Cria três thread pools distintos (producer, consumer, validation).
 *
 * O encerramento usa poison pills em DOIS NÍVEIS:
 *  1. Order.POISON_ORDER  na orderQueue   → faz OrderConsumer.run() sair do while(true)
 *  2. CustomThreadPool.POISON_PILL via shutdown() → faz Workers terminarem suas threads
 *
 * A ordem importa: producer encerra primeiro (não vai mais haver Orders novas);
 * depois injeta-se POISON_ORDER (libera consumers); depois encerra-se o consumer pool;
 * por fim, o validation pool (que só pode ser fechado quando ninguém mais vai submeter).
 */
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


        // Submetemos N OrderProducers (cada um one-shot) para gerar N pedidos. O paralelismo
        // de geração vem do número de workers do producer pool, não do número de submissões.
        CustomThreadPool ordersProducerPool = new CustomThreadPool(numberOfProducerThreads, taskQueueSize);
        for (int i = 0; i < numberOfOrders; i++) {
            ordersProducerPool.submit(
                    new OrderProducer(customerRepository, itemRepository, orderQueue)
            );
        }

        // Cada OrderConsumer é long-lived: ocupa um worker durante toda a vida do programa,
        // processando vários pedidos em loop até receber POISON_ORDER.
        CustomThreadPool ordersConsumerPool = new CustomThreadPool(numberOfConsumerThreads, taskQueueSize);
        for (int i = 0; i < numberOfConsumerThreads; i++) {
            ordersConsumerPool.submit(
                    new OrderConsumer(orderQueue, orderService, logisticsService)
            );
        }

        // SHUTDOWN COORDENADO

        // Producer pool aguarda todos os OrderProducer terminarem (cada um produz 1 pedido).
        // Após awaitTermination, garantimos que ninguém mais vai escrever em orderQueue.
        ordersProducerPool.shutdown();
        ordersProducerPool.awaitTermination();

        // Injeta uma POISON_ORDER por consumer thread.
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