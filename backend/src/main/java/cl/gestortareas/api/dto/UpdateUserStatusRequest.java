package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
    @NotNull Boolean active,
    @Size(max = 500) String reason
) {
}
