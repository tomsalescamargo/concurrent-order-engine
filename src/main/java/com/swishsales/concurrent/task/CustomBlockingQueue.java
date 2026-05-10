package com.swishsales.concurrent.task;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;


// Fila genérica controlada por semáforos.
public class CustomBlockingQueue<T> {

    private final Queue<T> queue = new LinkedList<>();

    private final Semaphore mutex = new Semaphore(1);

    private final Semaphore existingItems = new Semaphore(0); // Conta quantos itens existem na fila.

    private final Semaphore freeSpaces; // Conta quantos espaços livres existem na fila.

    public CustomBlockingQueue(int capacity) {
        this.freeSpaces = new Semaphore(capacity);
    }

    public void put(T item) throws InterruptedException {
        // Ve se tem espaço sobrando na fila
        freeSpaces.acquire();
        mutex.acquire();

        try {
            queue.add(item);
        } finally {
            mutex.release();
        }

        // Sinaliza que agora tem mais um item na fila
        existingItems.release();
    }

    public T take() throws InterruptedException {

        // Ve se tem item da fila pra ser pego
        existingItems.acquire();
        mutex.acquire();
        T item;
        try {
            // efetivamente pega o item da fila
            item = queue.poll();
        } finally {
            mutex.release();
        }
        // Sinaliza que abriu um espaço livre.
        freeSpaces.release();
        return item;
    }
}