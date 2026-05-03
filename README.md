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

## 🛠️ Build e Push das Aplicações (ECR)

Substitua `395380602553` pelo seu ID de conta AWS e `us-east-1` pela sua região.

### Login e Criação de Repositórios
```bash
# Login
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 395380602553.dkr.ecr.us-east-1.amazonaws.com

# Criar repositórios (uma única vez)
aws ecr create-repository --repository-name lab/customer-service --region us-east-1
aws ecr create-repository --repository-name lab/tracing-node --region us-east-1
```

### Aplicação Java (Customer Service)
```bash
cd applications/customer-service
chmod +x mvnw
./mvnw clean package
cd ../..
docker build -t lab/customer-service ./applications/customer-service
docker tag lab/customer-service:latest 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/customer-service:latest
docker push 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/customer-service:latest
```

### Aplicação Node.js (Tracing Node)
```bash
docker build -t lab/tracing-node ./applications/tracing-node
docker tag lab/tracing-node:latest 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/tracing-node:latest
docker push 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/tracing-node:latest
```

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
