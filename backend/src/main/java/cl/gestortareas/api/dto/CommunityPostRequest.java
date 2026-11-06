package cl.gestortareas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
    @Size(max = 150, message = "El título admite hasta 150 caracteres")
    String title,
    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 2000, message = "La publicación admite hasta 2000 caracteres")
    String content
) {}
