package com.example.myapp.dto;

import com.example.myapp.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating or fully replacing a todo (POST / PUT).
 */
public record TodoRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Size(max = 10000, message = "description is too long")
        String description,

        Boolean completed,

        Priority priority,

        LocalDate dueDate
) {
}
