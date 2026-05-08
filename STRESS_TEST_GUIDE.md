# Guia de Teste de Stress e Monitoramento (Local K8s)

Este guia descreve como realizar testes de stress em um ambiente local (Docker Desktop + WSL2) e monitorar o balanceamento de carga entre múltiplos pods.

## 1. Escalonamento e Verificação

Certifique-se de que o `tracing-node` está escalado para pelo menos 2 réplicas.

```bash
# Aplicar o manifesto atualizado
kubectl apply -f k8s/local/tracing-node.yaml

# Verificar se os pods estão rodando e em quais "nodes" (no Docker Desktop geralmente há apenas um node)
kubectl get pods -o wide -l app=tracing-node
```

## 2. Monitoramento de Logs em Tempo Real

Para confirmar se as requisições estão sendo balanceadas ou se estão "presas" em um único pod, use o seguinte comando para ver os logs de todos os pods do serviço simultaneamente:

```bash
# Monitorar logs de todos os pods com a label app=tracing-node
kubectl logs -l app=tracing-node -f --prefix
```

**O que observar:**
- Graças à alteração no código (`app.ts`), cada log agora exibe o hostname do pod: `[Pod: tracing-node-xxxxx]`.
- Se você vir mensagens de diferentes hostnames alternando, o balanceamento está funcionando.
- No gRPC, é comum ver todas as requisições indo para o mesmo pod devido ao reaproveitamento da conexão HTTP/2 (L4 balancing).

## 3. Simulação de Stress

### Usando ghz para gRPC
O `ghz` é excelente para testar a concorrência e forçar o balanceamento.

```bash
# Teste de 1000 requisições com concorrência de 10
ghz --insecure --proto ./applications/proto/tracing.proto \
    --call tracing.TracingService/GetTrace \
    -d '{"name":"StressTest"}' \
    -n 1000 -c 10 \
    localhost:50051
```

### Usando wrk para REST
```bash
# Teste de 30 segundos com 12 threads e 400 conexões abertas
wrk -t12 -c400 -d30s "http://localhost:8081/trace?name=StressTest"
```

---

# Configuração de Ingress para Balanceamento L7 (gRPC & REST)

Para que o gRPC seja balanceado corretamente entre os pods (L7), você precisa de um Ingress Controller que entenda o protocolo gRPC.

## 1. Instalar NGINX Ingress Controller
No Docker Desktop, você pode instalar facilmente:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

## 2. Criar Manifesto de Ingress (gRPC)
Crie um arquivo chamado `k8s/local/ingress-grpc.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tracing-ingress-grpc
  annotations:
    nginx.ingress.kubernetes.io/backend-protocol: "GRPC"
spec:
  ingressClassName: nginx
  rules:
  - host: tracing.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: tracing-node
            port:
              number: 50051
```

## 3. Configurar Host Local
Adicione ao seu arquivo `hosts` (no Windows em `C:\Windows\System32\drivers\etc\hosts` ou no WSL em `/etc/hosts`):
```text
127.0.0.1 tracing.local
```

## 4. Testar via Ingress
Agora, em vez de apontar para a porta do Service diretamente, aponte para o host do Ingress:

```bash
ghz --insecure --proto ./applications/proto/tracing.proto \
    --call tracing.TracingService/GetTrace \
    -d '{"name":"IngressTest"}' \
    tracing.local:80
```

**Nota:** O NGINX Ingress fará o balanceamento de cada chamada gRPC individualmente para diferentes pods, resolvendo o problema de "sticky connections" do HTTP/2.
