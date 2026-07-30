package com.example.myapp.dto;

import com.example.myapp.entity.Priority;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for partially updating a todo (PATCH). All fields are optional;
 * only non-null fields are applied.
 */
public record TodoPatchRequest(
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Size(max = 10000, message = "description is too long")
        String description,

        Boolean completed,

        Priority priority,

        LocalDate dueDate
) {
}
