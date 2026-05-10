package com.swishsales.concurrent.pool;

import com.swishsales.concurrent.task.CustomBlockingQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do padrão Thread Pool.
 *
 * O pool mantém N workers persistentes.
 * Cada worker fica em loop pegando Runnables de uma fila bloqueante interna e executando-os.
 * Isso evita o overhead de criar/destruir uma thread por tarefa.
 *
 */
public class CustomThreadPool {
    private final CustomBlockingQueue<Runnable> taskQueue;
    private final List<Worker> workers= new ArrayList<>();

    // flag de shutdown. Volatile pois várias threads leem.
    private volatile boolean shuttingDown = false;

    // Sentinela de fim de trabalho. Comparada por identidade (==) no worker.
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
    // Rejeitar pós-shutdown evita que tarefas submetidas tarde nunca rodem
    public void submit(Runnable task) throws InterruptedException {
        if (shuttingDown) {
            throw new IllegalStateException("Pool ja está em shutdown");
        }
        this.taskQueue.put(task);
    }

    // Graceful shutdown: enfileira uma POISON_PILL por worker. Cada worker eventualmente
    // pega uma e termina. Tarefas já enfileiradas ANTES das pílulas continuam sendo executadas
    // A flag é setada ANTES das pílulas para fechar a porta a novos submits durante a transição.
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

    /**
     * Worker: thread persistente que consome tarefas da taskQueue até receber POISON_PILL.
     * O try/catch(Throwable) ao redor de task.run() é defensivo. Sem ele,
     * uma exceção numa tarefa derrubaria a thread do worker, diminuindo silenciosamente
     * o tamanho efetivo do pool ao longo do tempo.
     */
    private class Worker extends Thread {
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

                // Detecta sinalização que acabou (graceful shutdown).
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
