package cl.gestortareas.api.dto;

import java.time.Instant;

public record UserSessionResponse(
    Long id, String device, String ipAddress, Instant createdAt,
    Instant lastUsedAt, Instant expiresAt, boolean current
) {
}

