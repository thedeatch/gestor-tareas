package cl.gestortareas.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.model.Task.Priority;
import cl.gestortareas.api.model.Task.Status;
import cl.gestortareas.api.service.CommunityTaskService;
import cl.gestortareas.api.service.CommunityTaskService.TaskView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/tasks")
public class CommunityTaskController {
    public record Create(@NotBlank @Size(max = 150) String description, @NotNull Priority priority,
                         LocalDate dueDate, Long assigneeId) {}
    public record Assignment(Long assigneeId) {}
    public record StatusChange(@NotNull Status status) {}

    private final CommunityTaskService tasks;

    public CommunityTaskController(CommunityTaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public List<TaskView> list(@PathVariable Long groupId, Authentication auth) {
        return tasks.list(groupId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long groupId, @Valid @RequestBody Create request, Authentication auth) {
        tasks.create(groupId, auth.getName(), request.description(), request.priority(),
            request.dueDate(), request.assigneeId());
    }

    @PatchMapping("/{id}/assignment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable Long groupId, @PathVariable Long id,
                       @RequestBody Assignment request, Authentication auth) {
        tasks.assign(groupId, id, auth.getName(), request.assigneeId());
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(@PathVariable Long groupId, @PathVariable Long id,
                       @Valid @RequestBody StatusChange request, Authentication auth) {
        tasks.changeStatus(groupId, id, auth.getName(), request.status());
    }
}