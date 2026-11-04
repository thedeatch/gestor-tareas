package cl.gestortareas.api.controller;

import cl.gestortareas.api.repository.TaskRepository;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTests {

    private static final String USER_EMAIL = "test@gestor.cl";

    private final MockMvc mockMvc;
    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;

    @Autowired
    TaskControllerIntegrationTests(
            MockMvc mockMvc,
            TaskRepository taskRepository,
            AppUserRepository userRepository
    ) {
        this.mockMvc = mockMvc;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new AppUser(
            "Usuario de prueba",
            USER_EMAIL,
            "hash-de-prueba"
        ));
    }

    @Test
    void shouldReturnEmptyTaskList() throws Exception {
        mockMvc.perform(get("/api/tasks").with(
                jwt().jwt(token -> token.subject(USER_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnUniformErrorWhenAuthenticationIsMissing()
        throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.path").value("/api/tasks"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturnFieldErrorsForInvalidTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "description": "",
                        "completed": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.description").isNotEmpty())
            .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    @Test
    void shouldReturnUniformErrorWhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 99999)
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.path").value("/api/tasks/99999"));
    }

    @Test
    void shouldCreateTask() throws Exception {
        String requestBody = """
                {
                    "description": "Crear desde prueba HTTP",
                    "completed": false
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().jwt(token -> token.subject(USER_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description")
                        .value("Crear desde prueba HTTP"))
                .andExpect(jsonPath("$.completed").value(false));

        assertEquals(1, taskRepository.count());
    }
    @Test
    void shouldUpdateTask() throws Exception {
        String createBody = """
                {
                    "description": "Tarea original",
                    "completed": false
                }
                """;

            mockMvc.perform(post("/api/tasks")
                    .with(jwt().jwt(token -> token.subject(USER_EMAIL)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
            .andExpect(status().isCreated());

        Long taskId = taskRepository.findAll().get(0).getId();

        String updateBody = """
                {
                    "description": "Tarea actualizada",
                    "completed": true
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(jwt().jwt(token -> token.subject(USER_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.description")
                        .value("Tarea actualizada"))
                .andExpect(jsonPath("$.completed").value(true));

        assertEquals(1, taskRepository.count());
    }
    @Test
    void shouldDeleteTask() throws Exception {
        String createBody = """
                {
                    "description": "Tarea que será eliminada",
                    "completed": false
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().jwt(token -> token.subject(USER_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        Long taskId = taskRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/tasks/{id}", taskId).with(
                jwt().jwt(token -> token.subject(USER_EMAIL))))
                .andExpect(status().isNoContent());

        assertEquals(0, taskRepository.count());
    }
}
