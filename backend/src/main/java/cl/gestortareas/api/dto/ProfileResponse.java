package cl.gestortareas.api.dto;

public record ProfileResponse(
    String name,
    String email,
    boolean hasAvatar
) {
}
