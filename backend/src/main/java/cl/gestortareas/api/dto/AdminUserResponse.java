package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.UserRole;

public record AdminUserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    boolean active,
    Instant createdAt
) {
}
