package cl.gestortareas.api.dto;

import java.time.Instant;

public record StoredFileResponse(
    Long id,
    String name,
    String contentType,
    long sizeBytes,
    String purpose,
    Instant createdAt
) {
}

