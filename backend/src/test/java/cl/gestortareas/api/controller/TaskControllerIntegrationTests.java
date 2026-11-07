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

    @Autowired org.springframework.jdbc.core.simple.JdbcClient jdbc;

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

    @Test
    void shouldKeepPersonalTaskClassificationPrivateAndPreserveTasksOnDeletion() throws Exception {
        var owner = jwt().jwt(token -> token.subject(USER_EMAIL));
        String otherEmail = "otras-tareas@gestor.cl";
        userRepository.save(new AppUser("Otra cuenta", otherEmail, "hash"));
        var other = jwt().jwt(token -> token.subject(otherEmail));
        String org = "/api/tasks/organization";
        mockMvc.perform(post(org).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind":"PROJECT","name":" Trabajo "}
                    """))
            .andExpect(status().isCreated());
        mockMvc.perform(post(org).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind":"PROJECT","name":"trabajo"}
                    """))
            .andExpect(status().isConflict());
        mockMvc.perform(post(org).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind":"CATEGORY","name":"Trabajo"}
                    """))
            .andExpect(status().isCreated());
        mockMvc.perform(get(org).with(owner)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Trabajo"));
        mockMvc.perform(get(org).with(other)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        var projectId = jdbc.sql("SELECT id FROM task_organization WHERE kind = 'PROJECT'")
            .query(Long.class).single();
        var categoryId = jdbc.sql("SELECT id FROM task_organization WHERE kind = 'CATEGORY'")
            .query(Long.class).single();

        String body = """
            {"description":"Tarea privada","completed":false}
            """;
        mockMvc.perform(post("/api/tasks").with(owner).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        var taskId = taskRepository.findAllByOwnerEmailOrderByIdAsc(USER_EMAIL).get(0).getId();
        var task = "/api/tasks/" + taskId;
        String classification = """
            {"projectId":%d,"categoryId":%d}
            """.formatted(projectId, categoryId);
        mockMvc.perform(put(task + "/classification").with(other).contentType(MediaType.APPLICATION_JSON)
                .content(classification)).andExpect(status().isNotFound());
        mockMvc.perform(put(task + "/classification").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content(classification)).andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(projectId))
            .andExpect(jsonPath("$.categoryId").value(categoryId));
        mockMvc.perform(put(task + "/classification").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId":%d}
                    """.formatted(categoryId))).andExpect(status().isBadRequest());
        mockMvc.perform(put(task).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Tarea editada","completed":true}
                    """)).andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(projectId))
            .andExpect(jsonPath("$.categoryId").value(categoryId));

        mockMvc.perform(delete(org + "/" + projectId).with(other)).andExpect(status().isNotFound());
        mockMvc.perform(delete(org + "/" + projectId).with(owner)).andExpect(status().isNoContent());
        mockMvc.perform(get(task).with(owner)).andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Tarea editada"))
            .andExpect(jsonPath("$.projectId").isEmpty())
            .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    @Test
    void shouldKeepAdvancedTaskDetailsCompatibleWithCompletionAndOwnership() throws Exception {
        var owner = jwt().jwt(token -> token.subject(USER_EMAIL));
        String otherEmail = "otra-tarea-avanzada@gestor.cl";
        userRepository.save(new AppUser("Otra cuenta", otherEmail, "hash"));
        var other = jwt().jwt(token -> token.subject(otherEmail));
        mockMvc.perform(post("/api/tasks").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Tarea original","completed":false}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("NORMAL"))
            .andExpect(jsonPath("$.status").value("PENDING"));
        var id = taskRepository.findAllByOwnerEmailOrderByIdAsc(USER_EMAIL).get(0).getId();
        var url = "/api/tasks/" + id;
        String details = """
            {"description":"Preparar entrega","priority":"HIGH","status":"IN_PROGRESS",
             "dueDate":"2027-01-02","tags":[" Backend ","backend","Entrega"]}
            """;
        mockMvc.perform(put(url + "/details").with(other).contentType(MediaType.APPLICATION_JSON)
                .content(details)).andExpect(status().isNotFound());
        mockMvc.perform(put(url + "/details").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content(details)).andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.completed").value(false))
            .andExpect(jsonPath("$.dueDate").value("2027-01-02"))
            .andExpect(jsonPath("$.tags.length()").value(2))
            .andExpect(jsonPath("$.tags[0]").value("Backend"));
        mockMvc.perform(put(url + "/details").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content(details.replace("Entrega", "Entrega, inválida"))).andExpect(status().isBadRequest());

        mockMvc.perform(put(url).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Editar sin completar","completed":false}
                    """)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(put(url).with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Completar","completed":true}
                    """)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.tags.length()").value(2));
        mockMvc.perform(put(url + "/details").with(owner).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Reabrir","priority":"LOW","status":"PENDING","dueDate":null,"tags":[]}
                    """)).andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(false))
            .andExpect(jsonPath("$.dueDate").isEmpty())
            .andExpect(jsonPath("$.tags.length()").value(0));
    }
}
