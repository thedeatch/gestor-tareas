package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.model.CommunityVisibility;

public record CommunitySummaryResponse(
    Long id,
    String name,
    String slug,
    String description,
    CommunityVisibility visibility,
    CommunityRole currentUserRole,
    long memberCount,
    Instant createdAt
) {}
