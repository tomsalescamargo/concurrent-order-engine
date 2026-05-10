package com.swishsales.concurrent.util;

public class CustomFuture<T> {
    private T result;
    private Exception exception;
    private volatile boolean isDone = false;

    public synchronized T get(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!isDone) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) throw new RuntimeException("Timeout aguardando Future");
            wait(remaining);
        }
        if (exception != null) throw exception;
        return result;
    }

    // Vai ser chamado quando termina o processamento
    public synchronized void complete(T result) {
        this.result = result;
        this.isDone = true;
        notifyAll(); // Acorda a thread do Consumer que estava dormindo no wait()
    }

    // Caso aconteça algum erro na validação
    public synchronized void completeExceptionally(Exception exception) {
        this.exception = exception;
        this.isDone = true;
        notifyAll(); // Acorda a thread do Consumer para ele lidar com o erro
    }
}