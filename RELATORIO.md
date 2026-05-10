# Relatório, Concurrent Order Engine

Trabalho 01, Programação Paralela e Distribuída, UFSC 2026.1.

**Membros do grupo:** Estéfano Tuyama, Gean Pereira, Tom Sales

---

## 1. Visão geral

O sistema simula um fluxo de venda concorrente. Clientes (threads produtoras) geram pedidos que são depositados em uma fila compartilhada. Threads consumidoras retiram pedidos da fila e os processam em três etapas sequenciais: validação de cadastro, validação financeira e logística de entrega. Cada etapa possui taxa de falha configurável, simulando rejeições reais (cadastro inválido, pagamento negado, avaria no transporte).

O foco do trabalho não está no domínio de vendas, mas em demonstrar a aplicação prática de três padrões de projeto para programação paralela, todos implementados manualmente sem uso de `java.util.concurrent.BlockingQueue`, `ExecutorService` ou `CompletableFuture` do JDK.

## 2. Estruturas de paralelismo em Java

A linguagem escolhida foi Java (versão 17). Diferentemente de C com pthreads (POSIX), as primitivas usadas vivem na JVM, mas mapeiam para threads nativas do sistema operacional, oferecendo paralelismo real em múltiplos núcleos.

Primitivas utilizadas:

| Primitiva Java | Equivalente POSIX | Uso no projeto |
|---|---|---|
| `Thread` (subclasse) | `pthread_create` + `pthread_join` | `Worker` no thread pool |
| `synchronized` + `wait`/`notifyAll` | `pthread_mutex` + `pthread_cond` | `CustomFuture` (monitor pattern) |
| `java.util.concurrent.Semaphore` | `sem_wait` / `sem_post` | `CustomBlockingQueue` (3 semáforos) |
| `volatile` | leitura/escrita atômica com barreira de memória | flag `shuttingDown` no pool |

A `Semaphore` do Java é semanticamente equivalente ao semáforo contador de Dijkstra, com `acquire()` correspondendo a `sem_wait` e `release()` a `sem_post`. Foi escolhida por mapear diretamente o pseudocódigo apresentado em aula.

## 3. Padrões de projeto adotados

| Padrão | Classe | Mecanismo |
|---|---|---|
| Producer-Consumer | `CustomBlockingQueue<T>` | 3 semáforos: mutex de buffer, contador de itens existentes, contador de espaços livres |
| Thread Pool | `CustomThreadPool` | N workers persistentes consumindo `Runnable`s de uma fila bloqueante interna |
| Future | `CustomFuture<T>` | Monitor pattern com `synchronized` + `wait`/`notifyAll`, com `volatile` para visibilidade e timeout para evitar bloqueio indefinido |

### 3.1 Producer-Consumer

Implementado inspirado no algoritmo clássico de Dijkstra apresentado em aula. O acesso ao buffer (`LinkedList<T>`) é protegido por um mutex de exclusão mútua. Dois semáforos contadores controlam o bloqueio: produtores aguardam em `freeSpaces` quando a fila está cheia; consumidores aguardam em `existingItems` quando a fila está vazia.

A ordem das operações `acquire` é crítica: o semáforo contador é adquirido sempre antes do mutex. A inversão abriria deadlock, pois uma thread poderia segurar o mutex enquanto espera por espaço ou item, impedindo a thread oposta de liberar a condição.

A fila é genérica (`<T>`), o que permite seu reuso pelo Thread Pool internamente para armazenar `Runnable`s.

### 3.2 Thread Pool

Cada pool mantém N workers persistentes, criados na construção. Cada worker executa um loop infinito que retira `Runnable`s da fila interna e os executa, evitando o overhead de criar e destruir uma thread por tarefa.

Decisões de implementação:

- **Reuso do Producer-Consumer:** a fila interna do pool é uma instância de `CustomBlockingQueue<Runnable>`. O método `submit` é o produtor, os workers são consumidores.
- **Graceful Shutdown:** uma sentinela `POISON_PILL` (Runnable singleton) é injetada na fila durante `shutdown()`, uma por worker. Workers detectam a sentinela por identidade (`==`) e terminam após processar tarefas pendentes.
- **Resiliência a falhas em tasks:** o `task.run()` é envolvido em `try/catch (Throwable)`. Sem essa proteção, uma exceção em qualquer tarefa derrubaria o worker, diminuindo silenciosamente o tamanho efetivo do pool ao longo do tempo.

### 3.3 Future

Implementação manual do padrão Future via monitor (synchronized + wait/notifyAll). A classe atua como um "recibo" para um valor que será calculado de forma assíncrona. A thread que precisa do resultado chama `get(timeoutMs)` e bloqueia até que a thread executora chame `complete(value)` ou `completeExceptionally(exception)`.

Pontos de atenção na implementação:

- O loop `while (!isDone)` ao redor do `wait()` protege contra spurious wakeups (acordares falsos do scheduler do SO) e contra signal hijacking.
- A flag `isDone` é `volatile`, garantindo visibilidade entre threads sem precisar de sincronização adicional para leituras.
- O timeout no `get()` previne que a thread chamadora fique permanentemente bloqueada caso a operação de fundo trave.

### 3.4 Composição dos três padrões

Os padrões se combinam no método `OrderService.validateOrder`, que demonstra o uso integrado:

1. Cria duas instâncias de `CustomFuture<Boolean>` (recibos vazios).
2. Submete duas tarefas ao `validationPool` (`CustomThreadPool`), cada uma completando uma das futures.
3. Aguarda ambas via `future.get(5000)`, com timeout de 5 segundos.

A submissão ao pool internamente usa o Producer-Consumer (a fila do pool); os workers do pool consomem e executam; cada execução completa um Future; a thread chamadora do consumer aguarda os Futures via monitor pattern.

## 4. Arquitetura

### 4.1 Fluxo de um pedido

```
[CLIENTES]                                         [SISTEMA DE VENDAS]
OrderProducer ──put()──▶ orderQueue ──take()──▶ OrderConsumer
  (one-shot)         (CustomBlockingQueue)            │
                                                      ▼
                                         OrderService.validateOrder()
                                                      │
                              ┌───────── submit() ────┴──── submit() ─────────┐
                              ▼                                                ▼
                  validationPool: validateData          validationPool: validatePayment
                              │ (~instantâneo)                                │ (~1s)
                              ▼                                                ▼
                       CustomFuture.complete()                     CustomFuture.complete()
                              └────────── future.get(5s) ──────────────────────┘
                                                      │
                                                      ▼ (se válido)
                                          LogisticsService.processDelivery()
                                                      │ (~2s)
                                                      ▼
                                          DELIVERED / FAILED_*
```

### 4.2 Estrutura de pacotes

```
src/main/java/com/swishsales/concurrent/
├── Main.java                           # ponto de entrada, orquestração
├── entity/                             # modelos de domínio
│   ├── Order.java                      # pedido + POISON_ORDER sentinela
│   ├── OrderStatus.java
│   ├── Customer.java
│   ├── Item.java
│   └── ItemBasketball.java
├── repository/                         # acesso a dados (in-memory)
├── service/
│   ├── OrderService.java               # validação (usa validationPool + futures)
│   └── LogisticsService.java
├── task/
│   ├── CustomBlockingQueue.java        # Producer-Consumer
│   ├── OrderProducer.java              # Runnable one-shot
│   └── OrderConsumer.java              # Runnable long-lived
├── pool/
│   └── CustomThreadPool.java           # Thread Pool
└── future/
    └── CustomFuture.java               # Future
```

A separação por pacotes reflete responsabilidades: `entity` e `repository` para o domínio, `service` para regras de negócio, e `task`/`pool`/`future` para os mecanismos de concorrência.

## 5. Decisões críticas de design

### 5.1 Três pools separados (consumer, producer, validation)

Os três pools são logicamente distintos e fisicamente separados em três instâncias de `CustomThreadPool`. A separação não é cosmética, ela previne **pool starvation deadlock**.

Cenário de deadlock se a separação fosse violada (mesmo pool para consumer e validation): os N workers do pool ficariam todos executando `OrderConsumer`, cada um bloqueado em `future.get(5000)` aguardando o resultado da validação. As tarefas de validação submetidas ao mesmo pool ficariam enfileiradas indefinidamente, sem worker disponível para executá-las. O resultado seria timeout em todos os pedidos após 5 segundos.

A regra geral aplicada: tarefas que aguardam por outras tarefas não devem rodar no mesmo pool.

### 5.2 Graceful shutdown em dois níveis (poison pills)

Cada fila bloqueante onde uma thread possa dormir requer um sinal próprio de fim de trabalho. O sistema possui duas filas bloqueantes onde threads aguardam: a `orderQueue` (consumers aguardando pedidos) e a `taskQueue` interna de cada pool (workers aguardando Runnables).

Por isso, o encerramento utiliza duas sentinelas:

1. **`Order.POISON_ORDER`**, injetada na `orderQueue` pelo `Main` após o producer pool terminar. Acorda os `OrderConsumer`s presos em `take()`, fazendo-os sair do `while(true)`.
2. **`CustomThreadPool.POISON_PILL`**, injetada pelo `shutdown()` do pool em sua taskQueue interna. Acorda os Workers, encerrando suas threads.

A ordem do shutdown no `Main` reflete a hierarquia de dependências:

```
producerPool   → shutdown + awaitTermination   (não haverá mais Orders novas)
orderQueue     → injetar N POISON_ORDER         (libera consumers)
consumerPool   → shutdown + awaitTermination   (encerra workers consumidores)
validationPool → shutdown + awaitTermination   (encerra após cessar submissões)
```

### 5.3 Dimensionamento do validation pool

Cada thread consumidora pode ter até duas validações concorrentes em voo (dados e pagamento, executadas em paralelo). Com N consumers ativos, o pico simultâneo é de 2N tarefas no validation pool. O dimensionamento `numberOfValidationPoolThreads = 2 * numberOfConsumerThreads` garante que validações nunca esperem por worker disponível, eliminando latência adicional de enfileiramento.

### 5.4 Threads superando o número de núcleos

A simulação usa `Thread.sleep` para emular latência de APIs externas (operadora financeira, transportadora). Threads em estado de espera não consomem CPU, são apenas guardadas pelo scheduler do sistema operacional. Por isso, o número total de threads do sistema (pools producer, consumer, validation) pode e deve exceder o número de núcleos físicos.

A regra "threads igual a núcleos" só vale para workloads CPU-bound (cálculo numérico, criptografia, compressão). Para workloads I/O-bound como este, mais threads aumentam o paralelismo efetivo até o ponto em que não haja mais trabalho a paralelizar.

### 5.5 Modelos opostos: Producer one-shot vs Consumer long-lived

`OrderProducer` é um Runnable one-shot: cada instância gera exatamente um pedido e termina. O paralelismo de produção emerge de submeter `numberOfOrders` instâncias ao producer pool.

`OrderConsumer` é um Runnable long-lived: cada instância contém um `while(true)` que processa pedidos continuamente. São submetidas exatamente `numberOfConsumerThreads` instâncias, uma por worker.

Essa assimetria é proposital. Producers representam "eventos" instantâneos (criação de um pedido); consumers representam "trabalhadores" persistentes do back-end.
