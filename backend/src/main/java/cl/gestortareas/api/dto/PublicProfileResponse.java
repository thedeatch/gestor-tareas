package cl.gestortareas.api.dto;

import java.time.Instant;
import java.util.List;

import cl.gestortareas.api.model.CommunityRole;

public record PublicProfileResponse(
    Long id,
    String name,
    String bio,
    boolean hasAvatar,
    Instant joinedAt,
    List<PublicCommunity> communities
) {
    public record PublicCommunity(
        Long id,
        String name,
        String slug,
        CommunityRole role
    ) {}
}
