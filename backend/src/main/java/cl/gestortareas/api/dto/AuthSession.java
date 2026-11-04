package cl.gestortareas.api.dto;

public record AuthSession(
    AuthResponse response,
    String refreshToken
) {
}
