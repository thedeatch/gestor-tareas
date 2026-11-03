package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.UserRole;

public record ProfileResponse(
    String name,
    String email,
    boolean hasAvatar,
    UserRole role
) {
}
