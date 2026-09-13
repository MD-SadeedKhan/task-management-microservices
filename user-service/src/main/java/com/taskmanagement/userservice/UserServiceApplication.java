package com.taskmanagement.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the User Service.
 *
 * Exposes a simple REST API (Controller -> Service -> Repository -> Entity)
 * backed by its own dedicated PostgreSQL database ("userdb"). This service
 * does not share a database with any other service and has no knowledge
 * of task-service beyond nothing at all - it is fully independent.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
