package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.CommunityAccessStatus;
import cl.gestortareas.api.model.CommunityAccessType;

public record CommunityAccessResponse(
    Long id,
    Long communityId,
    String communityName,
    boolean communityHasImage,
    Long userId,
    String userName,
    String userEmail,
    CommunityAccessType type,
    CommunityAccessStatus status,
    Instant createdAt
) {}
