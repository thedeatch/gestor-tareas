package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.service.TaskOrganizationService;
import cl.gestortareas.api.service.TaskOrganizationService.Item;
import cl.gestortareas.api.service.TaskOrganizationService.Kind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/tasks")
public class TaskOrganizationController {
    public record Create(@NotNull Kind kind, @NotBlank @Size(max = 80) String name) {}
    public record Rename(@NotBlank @Size(max = 80) String name) {}
    public record Classification(Long projectId, Long categoryId) {}

    private final TaskOrganizationService organization;

    public TaskOrganizationController(TaskOrganizationService organization) {
        this.organization = organization;
    }

    @GetMapping("/organization")
    public List<Item> list(Authentication auth) {
        return organization.list(auth.getName());
    }

    @PostMapping("/organization")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody Create request, Authentication auth) {
        organization.create(auth.getName(), request.kind(), request.name());
    }

    @PatchMapping("/organization/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable Long id, @Valid @RequestBody Rename request, Authentication auth) {
        organization.rename(auth.getName(), id, request.name());
    }

    @DeleteMapping("/organization/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        organization.delete(auth.getName(), id);
    }

    @PutMapping("/{id}/classification")
    public Task classify(@PathVariable Long id, @RequestBody Classification request, Authentication auth) {
        return organization.classify(auth.getName(), id, request.projectId(), request.categoryId());
    }
}