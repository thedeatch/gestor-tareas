package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.CommunityVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommunityRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String name,
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
    String description,
    @NotNull(message = "La visibilidad es obligatoria")
    CommunityVisibility visibility
) {}
