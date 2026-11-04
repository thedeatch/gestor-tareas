package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotBlank;
import cl.gestortareas.api.validation.ValidPassword;

public record ResetPasswordRequest(
    @NotBlank String token,
    @ValidPassword String password
) {
}

