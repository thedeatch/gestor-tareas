package cl.gestortareas.api.dto;

public record AuthResponse(
    String token,
    String email,
    String name
) {
}
