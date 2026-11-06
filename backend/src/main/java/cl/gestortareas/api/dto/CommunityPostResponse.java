package cl.gestortareas.api.dto;

import java.time.Instant;

public record CommunityPostResponse(
    Long id,
    Long authorId,
    String authorName,
    boolean authorHasAvatar,
    String title,
    String content,
    Instant createdAt,
    Instant updatedAt,
    boolean editable,
    boolean deletable
) {}
