package com.taskmanagement.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the API Gateway service.
 *
 * This service is built on Spring Cloud Gateway (WebFlux based) and is
 * responsible for routing external traffic to the internal microservices:
 *   - user-service (routes matching /users/**)
 *   - task-service (routes matching /tasks/**)
 *
 * No service discovery (Eureka/Consul) is used. Routes are statically
 * configured in application.yml using Docker Compose service names or
 * Kubernetes Service names as the target hosts.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
