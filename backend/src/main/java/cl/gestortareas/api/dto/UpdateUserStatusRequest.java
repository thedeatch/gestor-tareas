package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull Boolean active
) {
}
