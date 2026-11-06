package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityGuidelinesRequest(
    @NotNull @Size(max = 3000) String extendedDescription,
    @NotNull @Size(max = 5000) String rules
) {}