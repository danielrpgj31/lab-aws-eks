# Tutorial: Observabilidade no AWS EKS (OpenTelemetry, Jaeger, Prometheus, Grafana)

Este repositório contém o guia completo e os manifestos para provisionar um cluster Kubernetes na AWS com uma stack moderna de observabilidade.

## 🚀 Guia Rápido de Execução

### 1. Infraestrutura (EKS)
Utilize o `eksctl` para criar o cluster baseado no arquivo [cluster.yaml](file:///home/drjunior_br/dev/src/triangulo/lab-aws-eks/cluster.yaml):
```bash
eksctl create cluster -f cluster.yaml
```

### 2. Stack de Monitoramento (Prometheus & Grafana)
Instale via Helm:
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install obs-stack prometheus-community/kube-prometheus-stack
```

### 3. Distributed Tracing (Jaeger)
Aplique o manifesto do Jaeger:
```bash
kubectl apply -f k8s/jaeger.yaml
```

### 4. OpenTelemetry Collector
O hub central de telemetria:
```bash
kubectl apply -f k8s/otel-collector.yaml
```

---

## 🐳 Teste Local com Docker Compose

A forma mais simples de testar a comunicação entre as apps e o envio de traces antes de subir para a AWS é usando o Docker Compose.

### Como rodar:
```bash
# Na raiz do projeto
docker compose up --build
```

### O que testar:
1. **Chamada Java -> Node**: `curl "http://localhost:8080/trace?name=LocalTest"`
2. **Interface do Jaeger**: Acesse `http://localhost:16686` para ver os traces locais.

---

## 💻 Teste Local com Kubernetes (Docker Desktop / WSL2)

Se você estiver utilizando Docker Desktop com Kubernetes habilitado no Windows 11, utilize os manifestos preparados para imagens locais.

### 1. Build das Imagens Locais
Certifique-se de gerar as imagens com as tags corretas:
```bash
# Customer Service (Java)
cd applications/customer-service && ./mvnw clean package && cd ../..
docker build -t lab/customer-service ./applications/customer-service

# Tracing Node (Node.js)
docker build -t lab/tracing-node ./applications/tracing-node
```

### 2. Execução no Kubernetes Local
Aplique os manifestos da pasta `k8s/local`:
```bash
# Infraestrutura base (Jaeger e OTel)
kubectl apply -f k8s/jaeger.yaml
kubectl apply -f k8s/otel-collector.yaml

# Aplicações com imagens locais
kubectl apply -f k8s/local/customer-service.yaml
kubectl apply -f k8s/local/tracing-node.yaml
```

---

Para o procedimento detalhado de geração de imagens e ajustes para Amazon, consulte:
- [Guia de Deploy no Amazon ECR e Ajustes Kubernetes](ECR-DEPLOYMENT.md)

---

## 🔍 Verificação e Troubleshooting

### Como sensibilizar os traces?
1. Faça o port-forward das apps e do Jaeger:
   ```bash
   kubectl port-forward svc/customer-service 8080:8080
   kubectl port-forward svc/jaeger 16686:16686
   ```
2. Gere tráfego: `curl "http://localhost:8080/trace?name=Teste"`
3. Acesse o Jaeger: `http://localhost:16686`

### Lições Aprendidas (Troubleshooting)

| Problema | Causa | Solução |
| :--- | :--- | :--- |
| **otel-collector CrashLoopBackOff** | Uso do exportador `logging` obsoleto. | Alterar para `debug` com `verbosity: detailed`. |
| **tracing-node Error** | Falta do arquivo `instrumentation.ts`. | Criar o arquivo e usar `node -r ts-node/register`. |
| **Permission denied (npx)** | Falta de permissão de execução no container. | Usar o comando `node` diretamente no Dockerfile. |
| **Conflito de Portas** | App Node na 8081 vs K8s na 8080. | Padronizar porta 8081 nos manifestos e Dockerfile. |

---

## 📂 Estrutura do Projeto
- [k8s/](file:///home/drjunior_br/dev/src/triangulo/lab-aws-eks/k8s/): Manifestos Kubernetes.
- [applications/](file:///home/drjunior_br/dev/src/triangulo/lab-aws-eks/applications/): Código fonte das aplicações Java e Node.js.
- [cluster.yaml](file:///home/drjunior_br/dev/src/triangulo/lab-aws-eks/cluster.yaml): Definição da infraestrutura EKS.
