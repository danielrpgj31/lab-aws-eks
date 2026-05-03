# Atividades Concluídas - POC de Observabilidade

### 1 - Roteiro de Provisionamento
- [x] Documentação completa no README.md
- [x] Instruções de ECR e permissões
- [x] Guia de troubleshooting baseado em problemas reais encontrados

### 2 - POC Observabilidade (Node.js, Java, OpenTelemetry, Jaeger, Prometheus, Grafana)
- [x] **Infraestrutura**: Configuração de cluster EKS documentada.
- [x] **Traces**: Jaeger configurado e integrado via OTel Collector.
- [x] **Métricas**: Prometheus e Grafana instalados via Helm.
- [x] **Instrumentação Java**: App Spring Boot configurada com agente OTel.
- [x] **Instrumentação Node.js**: App Express configurada com SDK e auto-instrumentation.
- [x] **Coleta**: OTel Collector configurado com exportadores `debug` e `otlp`.

### 3 - Correções Realizadas (Bug Fixes)
- [x] Correção de sintaxe no `otel-collector.yaml` (depreciação do logging exporter).
- [x] Adição do arquivo `instrumentation.ts` para a aplicação Node.js.
- [x] Ajuste de portas e permissões nos Dockerfiles.
- [x] Padronização de nomes de repositórios no ECR.
