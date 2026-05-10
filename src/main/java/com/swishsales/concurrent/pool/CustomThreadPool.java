package com.swishsales.concurrent.pool;

import com.swishsales.concurrent.task.CustomBlockingQueue;

import java.util.ArrayList;
import java.util.List;

public class CustomThreadPool {
    private final CustomBlockingQueue<Runnable> taskQueue;
    private final List<Worker> workers= new ArrayList<>();

    // flag de shutdown. Volatile pois várias threads leem.
    private volatile boolean shuttingDown = false;

    // runnable pra sinalizar fim da linha pra uma thread
    private static final Runnable POISON_PILL= () -> {};

    public CustomThreadPool(int numberOfThreads, int queueCapacity) {
        this.taskQueue = new CustomBlockingQueue<>(queueCapacity);

       for (int i = 0; i < numberOfThreads; i++) {
           Worker w = new Worker("worker " + i);
           workers.add(w);
           w.start();
       }
    }

    // Manda uma task pra pool. Detecta se está em shutdown.
    public void submit(Runnable task) throws InterruptedException {
        if (shuttingDown) {
            throw new IllegalStateException("Pool ja está em shutdown");
        }
        this.taskQueue.put(task);
    }

    // Graceful shutdown -> Adiciona tasks POISON_PILL para as threads pararem de executar
    public void shutdown() throws InterruptedException {
        shuttingDown = true;
       for (int i = 0; i < workers.size(); i++) {
           taskQueue.put(POISON_PILL);
       }
    }

    // Trava aqui até todas as threads morrerem
    public void awaitTermination() throws InterruptedException {
        for  (Worker w : workers) {
            w.join();
        }
    }

    public class Worker extends Thread {
        public Worker(String name) { super(name); }


        @Override
        public void run() {
            // fica em loop procurando tasks pra executar.
            while (true) {
                Runnable task;

                try {
                    // Bloqueia até ter a task
                    task = taskQueue.take();
                } catch (InterruptedException e) {
                    // Interrompe a thread e sai do loop
                    Thread.currentThread().interrupt();
                    return;
                }

                // Detecta sinalização que acabou (graceful shutdown)
                if (task == POISON_PILL) {
                    return;
                }

                try {
                    task.run();
                } catch (Throwable t) { // Catch em Throwable (qualquer erro)
                    System.err.println("Erro ao executar task: " + t.getMessage());
                }
            }
        }
    }
}
