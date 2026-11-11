package cl.gestortareas.api.dto;

import java.time.Instant;
import java.util.List;

import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.model.CommunityVisibility;

public record CommunityDetailResponse(
    Long id,
    String name,
    String slug,
    String description,
    String extendedDescription,
    String rules,
    boolean hasImage,
    CommunityVisibility visibility,
    CommunityRole currentUserRole,
    Instant createdAt,
    List<CommunityMemberResponse> members
) {}
