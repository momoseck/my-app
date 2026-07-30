package com.example.myapp.dto;

import com.example.myapp.entity.Priority;
import com.example.myapp.entity.Todo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representation of a todo returned by the API.
 */
public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Priority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
