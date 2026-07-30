package com.example.myapp.controller;

import com.example.myapp.dto.PageResponse;
import com.example.myapp.dto.TodoPatchRequest;
import com.example.myapp.dto.TodoRequest;
import com.example.myapp.dto.TodoResponse;
import com.example.myapp.entity.Priority;
import com.example.myapp.entity.Todo;
import com.example.myapp.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "completed", "priority", "dueDate", "createdAt", "updatedAt");

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TodoResponse> list(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String sortField = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField));
        Page<Todo> result = service.list(completed, priority, search, pageable);
        return PageResponse.of(result, TodoResponse::from);
    }

    @GetMapping("/{id}")
    public TodoResponse get(@PathVariable Long id) {
        return TodoResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody TodoRequest request,
                                               UriComponentsBuilder uriBuilder) {
        Todo created = service.create(request);
        URI location = uriBuilder.path("/api/todos/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(TodoResponse.from(created));
    }

    @PutMapping("/{id}")
    public TodoResponse replace(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return TodoResponse.from(service.replace(id, request));
    }

    @PatchMapping("/{id}")
    public TodoResponse patch(@PathVariable Long id, @Valid @RequestBody TodoPatchRequest request) {
        return TodoResponse.from(service.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
