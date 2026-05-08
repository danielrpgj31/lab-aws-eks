# Tutorial: Análise, Monitoramento e Otimização de Performance (gRPC vs REST)

Este tutorial fornece um guia passo a passo para diagnosticar por que a comunicação REST pode estar superando o gRPC em seu ambiente e como otimizar ambos, com foco em gRPC.

## 1. Análise de Latência e Performance

### Por que o REST pode parecer mais rápido?
- **Overhead de Conexão:** gRPC utiliza HTTP/2. Se as conexões não forem mantidas vivas (Keep-Alive), o custo de negociação do HTTP/2 é maior que o HTTP/1.1.
- **Tamanho do Payload:** Para mensagens extremamente pequenas (como apenas uma string), o custo de serialização do Protobuf e os headers do HTTP/2 podem não compensar a economia de banda em comparação ao JSON.
- **Implementação Local:** Em ambientes de desenvolvimento (localhost), a latência de rede é quase zero, mascarando as vantagens de compactação do gRPC.

### Ferramentas de Benchmarking
Utilize ferramentas específicas para comparar a vazão (throughput) e latência:

- **Para REST:** [wrk](https://github.com/wg/wrk) ou [Apache Benchmark (ab)](https://httpd.apache.org/docs/2.4/programs/ab.html)
  ```bash
  # Exemplo com wrk
  wrk -t12 -c400 -d30s http://localhost:8080/trace-rest?name=Teste
  ```

- **Para gRPC:** [ghz](https://ghz.ax/)
  ```bash
  # Exemplo com ghz
  ghz --insecure --proto ./applications/proto/tracing.proto \
      --call tracing.TracingService/GetTrace \
      -d '{"name":"Teste"}' \
      localhost:50051
  ```

## 2. Monitoramento com OpenTelemetry e Jaeger

A aplicação já possui integração com OpenTelemetry. Use o Jaeger para visualizar onde o tempo está sendo gasto.

1. Acesse o Jaeger UI: `http://localhost:16686`
2. Filtre pelo serviço `customer-services`.
3. Compare os spans de `GET /trace-rest` e `GET /trace-grpc`.
4. Observe o tempo de "Request Outgoing" no Java e "Request Incoming" no Node.js.

**O que procurar:**
- Intervalos grandes entre o início do span no cliente e o início no servidor (problemas de rede/conexão).
- Tempo excessivo na serialização/deserialização.

## 3. Ajustes e Otimização

### Otimização no Cliente Java (customer-service)

#### A. Keep-Alive e Conexões Longas
Configure o `ManagedChannel` para manter a conexão ativa e evitar re-negociações constantes.

```java
this.channel = ManagedChannelBuilder.forAddress("tracing-node", 50051)
    .usePlaintext()
    .keepAliveTime(30, TimeUnit.SECONDS)
    .keepAliveTimeout(10, TimeUnit.SECONDS)
    .keepAliveWithoutCalls(true)
    .build();
```

#### B. Chamadas Assíncronas
Use o `FutureStub` ou `Stub` (async) em vez do `BlockingStub` para não bloquear a thread de execução do Spring, aumentando a concorrência.

```java
// Em vez de BlockingStub
TracingServiceGrpc.TracingServiceFutureStub futureStub = TracingServiceGrpc.newFutureStub(channel);
ListenableFuture<TraceResponse> future = futureStub.getTrace(request);
```

### Otimização no Servidor Node.js (tracing-node)

#### A. Ajuste de Pool de Threads e Conexões
O `@grpc/grpc-js` é single-threaded por padrão no loop de eventos do Node.js. Para alta carga, considere:
- Aumentar o `UV_THREADPOOL_SIZE`.
- Usar compressão se os payloads crescerem.

```typescript
// No servidor Node.js (opcional para mensagens pequenas)
const server = new grpc.Server({
  'grpc.max_receive_message_length': 1024 * 1024 * 10,
  'grpc.max_send_message_length': 1024 * 1024 * 10,
});
```

### Otimização de Infraestrutura (Kubernetes)

- **Load Balancing:** O Kubernetes Service (ClusterIP) faz balanceamento L4 (TCP). Como gRPC reutiliza a mesma conexão TCP (HTTP/2), todas as requisições de um Pod podem ir para o mesmo Pod de destino. 
- **Solução:** Utilize um Service Mesh (Linkerd/Istio) ou configure o cliente gRPC para balanceamento L7 (Client-side Load Balancing).

## 4. Checklist de Performance
1. [ ] As conexões gRPC são reutilizadas (Singleton Channel)?
2. [ ] O Keep-Alive está configurado para evitar timeouts de Idle no ELB/ALB da AWS?
3. [ ] A compressão (gzip) está habilitada para payloads > 1KB?
4. [ ] O pooling de threads no Java está dimensionado corretamente para o volume de requisições?
