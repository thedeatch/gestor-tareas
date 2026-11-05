package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.ThemePreference;

public record UserPreferencesResponse(
    ThemePreference theme,
    boolean reducedMotion
) {
}
