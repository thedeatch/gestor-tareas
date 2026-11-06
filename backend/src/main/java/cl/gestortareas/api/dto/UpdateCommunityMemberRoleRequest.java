package cl.gestortareas.api.dto;

import cl.gestortareas.api.model.CommunityRole;
import jakarta.validation.constraints.NotNull;

public record UpdateCommunityMemberRoleRequest(
    @NotNull CommunityRole role
) {}
