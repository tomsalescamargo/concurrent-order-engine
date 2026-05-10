# Concurrent Order Engine

Trabalho 01, Programação Paralela e Distribuída, UFSC 2026.1.

**Membros do grupo:** Estéfano Tuyama, Gean Pereira, Tom Sales

Protótipo de simulação de fluxo de venda concorrente, implementado em Java com três padrões de projeto para paralelismo (Producer-Consumer, Thread Pool, Future), todos escritos manualmente.

Para detalhes da arquitetura, decisões de design e padrões adotados, consulte [`RELATORIO.md`](./RELATORIO.md).

---

## Pré-requisitos

- Java 17 ou superior (`java -version`)
- Apache Maven 3.6+ (`mvn -version`)
- Ambiente com suporte a Swing (a entrada da taxa de erro usa `JOptionPane`)

## Compilação

Na raiz do projeto (onde está o `pom.xml`):

```bash
mvn compile
```

Para empacotar em jar:

```bash
mvn package
```

## Execução

```bash
java -cp target/classes com.swishsales.concurrent.Main
```

Ao iniciar, uma janela pedirá a taxa de erro em porcentagem (ex: `15` para 15% de chance de falha em cada etapa de validação e logística). O programa encerra automaticamente após processar todos os pedidos.

## Parâmetros de configuração

Editáveis no topo de `Main.java`:

| Parâmetro | Default | Descrição |
|---|---|---|
| `orderQueueSize` | 100 | Capacidade da fila producer/consumer. Quando cheia, producers bloqueiam. |
| `taskQueueSize` | 100 | Capacidade das filas internas dos pools producer e consumer. |
| `validationQueueSize` | 200 | Capacidade da fila interna do validation pool. |
| `numberOfProducerThreads` | 2 | Threads geradoras de pedidos. |
| `numberOfConsumerThreads` | 10 | Threads consumidoras. Principal alavanca de throughput. |
| `numberOfValidationPoolThreads` | 20 | Pool dedicado a validações. Default `2 × consumers`. |
| `numberOfOrders` | 100 | Total de pedidos da simulação. |
