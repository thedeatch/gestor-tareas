package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.UserRole;

public record AuthResponse(
    String token,
    String email,
    String name,
    UserRole role
) {
}
