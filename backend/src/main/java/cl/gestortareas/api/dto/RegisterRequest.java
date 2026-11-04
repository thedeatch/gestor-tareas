package cl.gestortareas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import cl.gestortareas.api.validation.ValidPassword;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank @Email String email,
    @ValidPassword String password
) {
}
