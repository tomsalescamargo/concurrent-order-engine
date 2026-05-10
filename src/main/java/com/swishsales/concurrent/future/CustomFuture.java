package com.swishsales.concurrent.future;

/**
 * Implementação customizada do padrão de projeto Future.
 * Atua como um "recibo" ou "promessa" de um valor que está sendo calculado de forma assíncrona
 * em outra Thread, permitindo que a Thread principal busque esse resultado no futuro.
 */
public class CustomFuture<T> {

    // Armazena o valor de retorno da operação após o processamento em background.
    private T result;

    // Armazena um eventual erro capturado pela thread de background.
    private Exception exception;

    // O modificador 'volatile' força a leitura e escrita desta variável diretamente na memória
    // principal (RAM), garantindo que alterações feitas por uma Thread sejam imediatamente
    // visíveis para outras Threads (evitando leituras desatualizadas do cache L1/L2 da CPU).
    private volatile boolean isDone = false;

    /**
     * Aguarda o processamento ser concluído para recuperar o resultado, com um limite de tempo.
     * O 'synchronized' garante o acesso exclusivo ao objeto (lock do monitor) para esta Thread.
     */
    public synchronized T get(long timeoutMs) throws Exception {
        // Calcula o tempo máximo absoluto que a Thread pode esperar.
        long deadline = System.currentTimeMillis() + timeoutMs;

        // O loop 'while' é essencial (ao invés de um 'if') para lidar com "Spurious Wakeups"
        // (acordares falsos do sistema operacional). A Thread sempre reavalia se a condição
        // (isDone) realmente foi satisfeita após acordar.
        while (!isDone) {
            long remaining = deadline - System.currentTimeMillis();

            // Se o tempo acabar antes do isDone virar true, interrompemos a espera com um erro.
            if (remaining <= 0) {
                throw new RuntimeException("Timeout aguardando Future");
            }

            // A Thread atual entra em estado TIMED_WAITING e LIBERA o lock do monitor do objeto.
            // Isso é fundamental para que a outra Thread consiga entrar no metodo complete() e finalizar o trabalho.
            wait(remaining);
        }

        // Se a operação paralela falhou, propagamos a exceção para quem invocou o get().
        if (exception != null) {
            throw exception;
        }

        // Retorna o resultado calculado no paralelismo.
        return result;
    }

    /**
     * Injeta o resultado final com sucesso.
     * Este metodo é chamado pela Thread validadora/trabalhadora ao terminar sua tarefa.
     */
    public synchronized void complete(T result) {
        this.result = result;
        this.isDone = true;

        // notifyAll() é o gatilho que avisa a JVM: "Acorde todas as Threads que estão suspensas no wait() deste objeto".
        notifyAll();
    }

    /**
     * Injeta um erro no Future, indicando que a operação paralela falhou.
     */
    public synchronized void completeExceptionally(Exception exception) {
        this.exception = exception;
        this.isDone = true;

        // Da mesma forma, acorda a Thread consumidora, mas agora ela encontrará uma exceção ao analisar a variável 'exception'.
        notifyAll();
    }
}