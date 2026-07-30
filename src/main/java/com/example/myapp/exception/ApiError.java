package com.example.myapp.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body returned by the API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
