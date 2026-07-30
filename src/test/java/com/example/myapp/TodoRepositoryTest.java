package com.example.myapp;

import com.example.myapp.entity.Priority;
import com.example.myapp.entity.Todo;
import com.example.myapp.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TodoRepositoryTest {

    @Autowired
    private TodoRepository repository;

    private Todo newTodo(String title, boolean completed, Priority priority) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setPriority(priority);
        return todo;
    }

    @Test
    void savesAndReadsBackTimestamps() {
        Todo saved = repository.save(newTodo("Buy milk", false, Priority.HIGH));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void savesAndCountsTodos() {
        repository.save(newTodo("Task A", false, Priority.HIGH));
        repository.save(newTodo("Task B", true, Priority.LOW));
        repository.save(newTodo("Task C", false, Priority.LOW));

        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findAll()).hasSize(3);
    }
}
