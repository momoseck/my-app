package com.example.myapp.service;

import com.example.myapp.dto.TodoPatchRequest;
import com.example.myapp.dto.TodoRequest;
import com.example.myapp.entity.Priority;
import com.example.myapp.entity.Todo;
import com.example.myapp.exception.TodoNotFoundException;
import com.example.myapp.repository.TodoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TodoService {

    private final TodoRepository repository;

    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Todo> list(Boolean completed, Priority priority, String search, Pageable pageable) {
        Specification<Todo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (completed != null) {
                predicates.add(cb.equal(root.get("completed"), completed));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.<String>get("title")), like);
                Predicate descMatch = cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like);
                predicates.add(cb.or(titleMatch, descMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Todo get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional
    public Todo create(TodoRequest request) {
        Todo todo = new Todo();
        todo.setTitle(request.title().trim());
        todo.setDescription(request.description());
        todo.setCompleted(request.completed() != null && request.completed());
        todo.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        todo.setDueDate(request.dueDate());
        return repository.save(todo);
    }

    @Transactional
    public Todo replace(Long id, TodoRequest request) {
        Todo todo = get(id);
        todo.setTitle(request.title().trim());
        todo.setDescription(request.description());
        todo.setCompleted(request.completed() != null && request.completed());
        todo.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        todo.setDueDate(request.dueDate());
        return repository.save(todo);
    }

    @Transactional
    public Todo patch(Long id, TodoPatchRequest request) {
        Todo todo = get(id);
        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("title must not be blank");
            }
            todo.setTitle(title);
        }
        if (request.description() != null) {
            todo.setDescription(request.description());
        }
        if (request.completed() != null) {
            todo.setCompleted(request.completed());
        }
        if (request.priority() != null) {
            todo.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            todo.setDueDate(request.dueDate());
        }
        return repository.save(todo);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
