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
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.TaskRepository;

@SpringBootTest
class TaskServiceIntegrationTests {

    private static final String USER_EMAIL = "service-test@gestor.cl";

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;
    
    @Autowired
    TaskServiceIntegrationTests(
        TaskService taskService,
        TaskRepository taskRepository,
        AppUserRepository userRepository
    ) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new AppUser(USER_EMAIL, "hash-de-prueba"));
    }

    @Test
    void shouldCreateTask() {
        Task newTask = new Task(
            "Aprender pruebas con Spring Boot",
            false
        );

        Task createdTask = taskService.create(newTask, USER_EMAIL);

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
            new Task("Primera tarea", false), USER_EMAIL
        );

        taskService.create(
            new Task("Segunda tarea", true), USER_EMAIL
        );

        List<Task> tasks = taskService.findAll(USER_EMAIL);

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
            new Task("Buscar esta tarea", false), USER_EMAIL
        );

        Task foundTask = taskService.findById(
            createdTask.getId(), USER_EMAIL
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
            () -> taskService.findById(999L, USER_EMAIL)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            exception.getStatusCode()
        );
    } 
    @Test
    void shouldUpdateTask() {
        Task createdTask = taskService.create(
            new Task("Descripción original", false), USER_EMAIL
        );

        Task newValues = new Task(
            "Descripción actualizada",
            true
        );

        Task updatedTask = taskService.update(
            createdTask.getId(),
            newValues,
            USER_EMAIL
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
            new Task("Tarea para eliminar", false), USER_EMAIL
        );

        taskService.delete(createdTask.getId(), USER_EMAIL);

        boolean exists = taskRepository.existsById(
            createdTask.getId()
        );

        assertFalse(exists);
    }

    @Test
    void shouldNotExposeTasksOwnedByAnotherUser() {
        String otherEmail = "otro@gestor.cl";
        userRepository.save(new AppUser(otherEmail, "otro-hash"));

        taskService.create(
            new Task("Tarea del usuario principal", false),
            USER_EMAIL
        );
        Task otherTask = taskService.create(
            new Task("Tarea privada de otro usuario", false),
            otherEmail
        );

        List<Task> visibleTasks = taskService.findAll(USER_EMAIL);

        assertEquals(1, visibleTasks.size());
        assertEquals(
            "Tarea del usuario principal",
            visibleTasks.get(0).getDescription()
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> taskService.findById(otherTask.getId(), USER_EMAIL)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
