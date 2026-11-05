package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.NotificationType;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String message,
    String targetUrl,
    String actorName,
    String actorEmail,
    Instant readAt,
    Instant createdAt
) {
}
