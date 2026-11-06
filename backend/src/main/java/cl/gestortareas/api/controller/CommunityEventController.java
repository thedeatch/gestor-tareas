package cl.gestortareas.api.controller;

import java.time.OffsetDateTime;
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

import cl.gestortareas.api.service.CommunityEventService;
import cl.gestortareas.api.service.CommunityEventService.Event;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/events")
public class CommunityEventController {
    public record CreateEvent(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull OffsetDateTime startsAt
    ) {}

    private final CommunityEventService events;

    public CommunityEventController(CommunityEventService events) {
        this.events = events;
    }

    @GetMapping
    public List<Event> findAll(@PathVariable Long groupId, Authentication auth) {
        return events.findAll(groupId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long groupId, @Valid @RequestBody CreateEvent request,
                       Authentication auth) {
        events.create(groupId, auth.getName(), request.title(), request.description(), request.startsAt());
    }

    @PutMapping("/{eventId}/registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@PathVariable Long groupId, @PathVariable Long eventId, Authentication auth) {
        events.setRegistration(groupId, eventId, auth.getName(), true);
    }

    @DeleteMapping("/{eventId}/registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long groupId, @PathVariable Long eventId, Authentication auth) {
        events.setRegistration(groupId, eventId, auth.getName(), false);
    }
}