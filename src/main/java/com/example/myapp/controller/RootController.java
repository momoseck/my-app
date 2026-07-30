package com.example.myapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Small landing endpoint describing the API.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "name", "Todolist API",
                "status", "ok",
                "endpoints", List.of(
                        "GET    /api/todos",
                        "POST   /api/todos",
                        "GET    /api/todos/{id}",
                        "PUT    /api/todos/{id}",
                        "PATCH  /api/todos/{id}",
                        "DELETE /api/todos/{id}",
                        "GET    /actuator/health"
                )
        );
    }
}
