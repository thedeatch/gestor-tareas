package cl.gestortareas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCommunityInvitationRequest(
    @NotBlank @Email String email
) {}
