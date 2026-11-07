package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import cl.gestortareas.api.service.SubtaskService;
import cl.gestortareas.api.service.SubtaskService.Subtask;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping({"/api/tasks/{taskId}/subtasks", "/api/communities/{groupId}/tasks/{taskId}/subtasks"})
public class SubtaskController {
    public record Create(@NotBlank @Size(max = 150) String description) {}
    public record Update(@NotBlank @Size(max = 150) String description, @NotNull Boolean completed) {}

    private final SubtaskService subtasks;

    public SubtaskController(SubtaskService subtasks) {
        this.subtasks = subtasks;
    }

    @GetMapping
    public List<Subtask> list(@PathVariable(required = false) Long groupId,
                             @PathVariable Long taskId, Authentication auth) {
        return subtasks.list(groupId, taskId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable(required = false) Long groupId, @PathVariable Long taskId,
                       @Valid @RequestBody Create request, Authentication auth) {
        subtasks.create(groupId, taskId, auth.getName(), request.description());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable(required = false) Long groupId, @PathVariable Long taskId,
                       @PathVariable Long id, @Valid @RequestBody Update request, Authentication auth) {
        subtasks.update(groupId, taskId, id, auth.getName(), request.description(), request.completed());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(required = false) Long groupId, @PathVariable Long taskId,
                       @PathVariable Long id, Authentication auth) {
        subtasks.delete(groupId, taskId, id, auth.getName());
    }
}