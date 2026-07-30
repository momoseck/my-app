package com.example.myapp.exception;

/**
 * Thrown when a todo with the given id does not exist.
 */
public class TodoNotFoundException extends RuntimeException {
    public TodoNotFoundException(Long id) {
        super("Todo not found with id " + id);
    }
}
