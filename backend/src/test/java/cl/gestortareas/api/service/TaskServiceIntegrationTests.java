package cl.gestortareas.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.repository.TaskRepository;

@SpringBootTest
class TaskServiceIntegrationTests {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    
    @Autowired
    TaskServiceIntegrationTests(
        TaskService taskService,
        TaskRepository taskRepository
    ) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void shouldCreateTask() {
        Task newTask = new Task(
            "Aprender pruebas con Spring Boot",
            false
        );

        Task createdTask = taskService.create(newTask);

        assertNotNull(createdTask.getId());
        assertEquals(
            "Aprender pruebas con Spring Boot",
            createdTask.getDescription()
        );
        assertFalse(createdTask.isCompleted());
    }

    @Test
    void shouldFindAllTasks() {
        taskService.create(
            new Task("Primera tarea", false)
        );

        taskService.create(
            new Task("Segunda tarea", true)
        );

        List<Task> tasks = taskService.findAll();

        assertEquals(2, tasks.size());
        assertEquals(
            "Primera tarea",
            tasks.get(0).getDescription()
        );
        assertEquals(
            "Segunda tarea",
            tasks.get(1).getDescription()
        );
    }
    @Test
    void shouldFindTaskById() {
        Task createdTask = taskService.create(
            new Task("Buscar esta tarea", false)
        );

        Task foundTask = taskService.findById(
            createdTask.getId()
        );

        assertEquals(
            createdTask.getId(),
            foundTask.getId()
        );

        assertEquals(
            "Buscar esta tarea",
            foundTask.getDescription()
        );
    }
    @Test
    void shouldReturnNotFoundWhenTaskDoesNotExist() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> taskService.findById(999L)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            exception.getStatusCode()
        );
    } 
    @Test
    void shouldUpdateTask() {
        Task createdTask = taskService.create(
            new Task("Descripción original", false)
        );

        Task newValues = new Task(
            "Descripción actualizada",
            true
        );

        Task updatedTask = taskService.update(
            createdTask.getId(),
            newValues
        );

        assertEquals(
            createdTask.getId(),
            updatedTask.getId()
        );

        assertEquals(
            "Descripción actualizada",
            updatedTask.getDescription()
        );

        assertTrue(updatedTask.isCompleted());
    }
    @Test
    void shouldDeleteTask() {
        Task createdTask = taskService.create(
            new Task("Tarea para eliminar", false)
        );

        taskService.delete(createdTask.getId());

        boolean exists = taskRepository.existsById(
            createdTask.getId()
        );

        assertFalse(exists);
    }
}