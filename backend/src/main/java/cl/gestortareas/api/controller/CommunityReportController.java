package cl.gestortareas.api.controller;

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

import cl.gestortareas.api.service.CommunityReportService;
import cl.gestortareas.api.service.CommunityReportService.Report;
import cl.gestortareas.api.service.CommunityReportService.Resolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/reports")
public class CommunityReportController {
    public record Create(@NotNull Long postId, Long commentId, @NotBlank @Size(max = 500) String reason) {}
    public record Resolve(@NotNull Resolution resolution, @NotBlank @Size(max = 500) String reason) {}

    private final CommunityReportService reports;

    public CommunityReportController(CommunityReportService reports) {
        this.reports = reports;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long groupId, @Valid @RequestBody Create request, Authentication auth) {
        reports.create(groupId, request.postId(), request.commentId(), request.reason(), auth.getName());
    }

    @GetMapping
    public List<Report> pending(@PathVariable Long groupId, Authentication auth) {
        return reports.pending(groupId, auth.getName());
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable Long groupId, @PathVariable Long id,
                         @Valid @RequestBody Resolve request, Authentication auth) {
        reports.resolve(groupId, id, request.resolution(), request.reason(), auth.getName());
    }
}