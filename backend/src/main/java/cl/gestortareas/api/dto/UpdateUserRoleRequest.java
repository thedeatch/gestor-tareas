package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRoleRequest(
    @NotNull UserRole role,
    @Size(max = 500) String reason
) {
}
