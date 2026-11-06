package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.CommunityAccessStatus;

public record CommunityDiscoveryResponse(
    Long id,
    String name,
    String slug,
    String description,
    long memberCount,
    CommunityAccessStatus requestStatus
) {}
