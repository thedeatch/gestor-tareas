package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/users/me/notification-preferences")
public class NotificationPreferenceController {
    public record Update(@NotNull Boolean enabled) {}

    private final NotificationPreferenceService preferences;

    public NotificationPreferenceController(NotificationPreferenceService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public List<NotificationType> disabled(Authentication auth) {
        return preferences.disabled(auth.getName());
    }

    @PutMapping("/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable NotificationType type, @Valid @RequestBody Update request, Authentication auth) {
        preferences.set(auth.getName(), type, request.enabled());
    }
}