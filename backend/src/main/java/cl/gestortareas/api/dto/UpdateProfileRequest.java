package cl.gestortareas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank String currentPassword,
    @Size(max = 300) String bio,
    Boolean publicProfile,
    Boolean showPublicCommunities
) {
}
