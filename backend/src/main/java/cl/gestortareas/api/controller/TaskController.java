package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.dto.TaskDetailsRequest;
import cl.gestortareas.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(
    name = "Tareas",
    description = "Operaciones CRUD para administrar tareas"
)
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Obtener todas las tareas")
    @ApiResponse(
        responseCode = "200",
        description = "Listado de tareas obtenido correctamente"
    )
    @GetMapping
    public List<Task> findAll(Authentication authentication) {
        return taskService.findAll(authentication.getName());
    }

    @Operation(summary = "Obtener una tarea por su identificador")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tarea encontrada"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tarea no encontrada"
        )
    })
    @GetMapping("/{id}")
    public Task findById(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return taskService.findById(id, authentication.getName());
    }

    @Operation(summary = "Crear una nueva tarea")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Tarea creada correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de la tarea invalidos"
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(
        @Valid @RequestBody Task task,
        Authentication authentication
    ) {
        return taskService.create(task, authentication.getName());
    }

    @Operation(summary = "Actualizar una tarea existente")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tarea actualizada correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de la tarea invalidos"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tarea no encontrada"
        )
    })
    @PutMapping("/{id}")
    public Task update(
        @PathVariable Long id,
        @Valid @RequestBody Task task,
        Authentication authentication
    ) {
        return taskService.update(id, task, authentication.getName());
    }

    @PutMapping("/{id}/details")
    public Task updateDetails(@PathVariable Long id, @Valid @RequestBody TaskDetailsRequest request,
                              Authentication authentication) {
        return taskService.updateDetails(id, request, authentication.getName());
    }

    @Operation(summary = "Eliminar una tarea")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Tarea eliminada correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tarea no encontrada"
        )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id,
        Authentication authentication
    ) {
        taskService.delete(id, authentication.getName());
    }
}
