# Guia de Deploy no Amazon ECR e Ajustes Kubernetes

Este documento descreve o procedimento para geração de imagens Docker, publicação no Amazon ECR e atualização dos manifestos Kubernetes para utilizar essas imagens.

## 1. Preparação e Autenticação

Antes de começar, certifique-se de ter o AWS CLI configurado. Substitua `395380602553` pelo seu ID de conta AWS e `us-east-1` pela sua região.

### Login no ECR
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 395380602553.dkr.ecr.us-east-1.amazonaws.com
```

### Criação de Repositórios (Executar apenas uma vez)
Se os repositórios ainda não existirem, crie-os:
```bash
aws ecr create-repository --repository-name lab/customer-service --region us-east-1
aws ecr create-repository --repository-name lab/tracing-node --region us-east-1
```

## 2. Build e Push das Imagens

### Aplicação Java (Customer Service)
1. Navegue até a pasta da aplicação e gere o pacote JAR:
   ```bash
   cd applications/customer-service
   chmod +x mvnw
   ./mvnw clean package
   cd ../..
   ```
2. Build da imagem Docker:
   ```bash
   docker build -t lab/customer-service ./applications/customer-service
   ```
3. Tag e Push para o ECR:
   ```bash
   docker tag lab/customer-service:latest 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/customer-service:latest
   docker push 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/customer-service:latest
   ```

### Aplicação Node.js (Tracing Node)
1. Build da imagem Docker:
   ```bash
   docker build -t lab/tracing-node ./applications/tracing-node
   ```
2. Tag e Push para o ECR:
   ```bash
   docker tag lab/tracing-node:latest 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/tracing-node:latest
   docker push 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/tracing-node:latest
   ```

## 3. Ajustes no Kubernetes (K8s)

Após realizar o push das imagens, é necessário garantir que os manifestos do Kubernetes apontem para as URLs corretas do ECR.

### Atualizando os Manifestos
Os arquivos de deployment estão localizados na pasta `k8s/`. Você deve editar o campo `image` nos seguintes arquivos:

- **[customer-service.yaml](k8s/customer-service.yaml)**:
  ```yaml
  spec:
    containers:
    - name: customer-service
      image: 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/customer-service:latest
  ```

- **[tracing-node.yaml](k8s/tracing-node.yaml)**:
  ```yaml
  spec:
    containers:
    - name: tracing-node
      image: 395380602553.dkr.ecr.us-east-1.amazonaws.com/lab/tracing-node:latest
  ```

### Aplicando as Mudanças
Após salvar os arquivos, aplique as atualizações no cluster:
```bash
kubectl apply -f k8s/customer-service.yaml
kubectl apply -f k8s/tracing-node.yaml
```

Caso o deployment já esteja rodando, você pode forçar o restart dos pods para garantir que a nova imagem seja baixada (se houver alteração na tag):
```bash
kubectl rollout restart deployment customer-service
kubectl rollout restart deployment tracing-node
```
