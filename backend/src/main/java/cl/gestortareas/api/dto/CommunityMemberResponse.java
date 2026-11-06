package cl.gestortareas.api.dto;

import java.time.Instant;

import cl.gestortareas.api.model.CommunityRole;

public record CommunityMemberResponse(
    Long userId,
    String name,
    String email,
    CommunityRole role,
    Instant joinedAt
) {}
