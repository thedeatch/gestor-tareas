package cl.gestortareas.api.controller;

import java.time.LocalDate;
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
import cl.gestortareas.api.service.CommunityGoalService;
import cl.gestortareas.api.service.CommunityGoalService.Overview;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/goals")
public class CommunityGoalController {
    public record GoalRequest(@NotBlank @Size(max = 150) String title,
                              @NotNull @Size(max = 2000) String description,
                              LocalDate dueDate, @NotNull List<@NotNull Long> taskIds) {}
    private final CommunityGoalService goals;

    public CommunityGoalController(CommunityGoalService goals) {
        this.goals = goals;
    }

    @GetMapping
    public Overview findAll(@PathVariable Long groupId, Authentication auth) {
        return goals.findAll(groupId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Overview create(@PathVariable Long groupId, @Valid @RequestBody GoalRequest request, Authentication auth) {
        return goals.create(groupId, auth.getName(), request.title(), request.description(), request.dueDate(), request.taskIds());
    }

    @PutMapping("/{goalId}")
    public Overview update(@PathVariable Long groupId, @PathVariable Long goalId,
                           @Valid @RequestBody GoalRequest request, Authentication auth) {
        return goals.update(groupId, goalId, auth.getName(), request.title(), request.description(), request.dueDate(), request.taskIds());
    }

    @DeleteMapping("/{goalId}")
    public Overview delete(@PathVariable Long groupId, @PathVariable Long goalId, Authentication auth) {
        return goals.delete(groupId, goalId, auth.getName());
    }
}
