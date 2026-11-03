package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull UserRole role
) {
}
