package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferencesRequest(
    @NotNull ThemePreference theme,
    boolean reducedMotion
) {
}
