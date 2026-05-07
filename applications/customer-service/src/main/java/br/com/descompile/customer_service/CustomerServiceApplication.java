package br.com.descompile.customer_service;

import br.com.descompile.customer_service.grpc.TraceRequest;
import br.com.descompile.customer_service.grpc.TraceResponse;
import br.com.descompile.customer_service.grpc.TracingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@RestController
public class CustomerServiceApplication {

    private final RestTemplate restTemplate = new RestTemplate();
    private ManagedChannel channel;
    private TracingServiceGrpc.TracingServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        // Configuração do canal gRPC
        this.channel = ManagedChannelBuilder.forAddress("tracing-node", 50051)
                .usePlaintext()
                .build();
        this.blockingStub = TracingServiceGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    // Endpoint original que agora chamaremos de gRPC por padrão, ou mantemos para compatibilidade
    @GetMapping("/trace")
    public String startTrace(@RequestParam String name) {
        return "Default (gRPC) -> " + callGrpc(name);
    }

    @GetMapping("/trace-grpc")
    public String startTraceGrpc(@RequestParam String name) {
        return "Explicit gRPC -> " + callGrpc(name);
    }

    @GetMapping("/trace-rest")
    public String startTraceRest(@RequestParam String name) {
        try {
            // Chamada REST para o tracing-node na porta 8081
            String response = restTemplate.getForObject("http://tracing-node:8081/trace?name=" + name, String.class);
            return "Java REST says: " + response;
        } catch (Exception e) {
            return "Error calling REST: " + e.getMessage();
        }
    }

    private String callGrpc(String name) {
        try {
            TraceRequest request = TraceRequest.newBuilder().setName(name).build();
            TraceResponse response = blockingStub.getTrace(request);
            return "Java gRPC says: " + response.getMessage();
        } catch (Exception e) {
            return "Error calling gRPC: " + e.getMessage();
        }
    }

}
