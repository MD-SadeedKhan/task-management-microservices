package com.taskmanagement.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Task Service.
 *
 * Exposes a simple REST API (Controller -> Service -> Repository -> Entity)
 * backed by its own dedicated PostgreSQL database ("taskdb"). Tasks reference
 * a user only via a plain "userId" (Long) field - there is intentionally no
 * JPA relationship or network call to user-service.
 */
@SpringBootApplication
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
