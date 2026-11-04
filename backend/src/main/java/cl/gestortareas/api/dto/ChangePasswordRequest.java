package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotBlank;
import cl.gestortareas.api.validation.ValidPassword;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @ValidPassword String newPassword
) {
}
