package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.AdminAuditAction;

public record AdminAuditResponse(
    Long id,
    String actorEmail,
    Long targetUserId,
    String targetEmail,
    AdminAuditAction action,
    String previousValue,
    String newValue,
    String reason,
    Instant createdAt
) {
}
