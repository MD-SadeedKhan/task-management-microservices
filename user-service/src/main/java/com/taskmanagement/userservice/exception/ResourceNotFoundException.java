package com.taskmanagement.userservice.exception;

/**
 * Thrown when a requested user cannot be found.
 * Translated to an HTTP 404 response by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
