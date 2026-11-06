package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import cl.gestortareas.api.dto.*;
import cl.gestortareas.api.service.CommunityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Grupos", description = "Grupos internos de la comunidad y sus membresías")
@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<CommunitySummaryResponse> findMine(Authentication authentication) {
        return communityService.findMine(authentication.getName());
    }

    @GetMapping("/{id}")
    public CommunityDetailResponse findById(@PathVariable Long id,
                                            Authentication authentication) {
        return communityService.findById(id, authentication.getName());
    }

    @GetMapping("/discover")
    public List<CommunityDiscoveryResponse> discover(Authentication authentication) {
        return communityService.discover(authentication.getName());
    }

    @PostMapping("/{id}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityAccessResponse requestToJoin(@PathVariable Long id,
                                                  Authentication authentication) {
        return communityService.requestToJoin(id, authentication.getName());
    }

    @DeleteMapping("/{id}/join-requests/mine")
    public CommunityAccessResponse cancelMyJoinRequest(
        @PathVariable Long id, Authentication authentication) {
        return communityService.cancelJoinRequest(id, authentication.getName());
    }

    @GetMapping("/{id}/join-requests")
    public List<CommunityAccessResponse> pendingJoinRequests(
        @PathVariable Long id, Authentication authentication) {
        return communityService.pendingJoinRequests(id, authentication.getName());
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public CommunityMemberResponse updateMemberRole(
        @PathVariable Long id,
        @PathVariable Long userId,
        @Valid @RequestBody UpdateCommunityMemberRoleRequest request,
        Authentication authentication
    ) {
        return communityService.updateMemberRole(
            id, userId, request.role(), authentication.getName());
    }

    @PatchMapping("/{id}/join-requests/{requestId}/approve")
    public CommunityAccessResponse approveJoinRequest(@PathVariable Long id,
        @PathVariable Long requestId, Authentication authentication) {
        return communityService.resolveJoinRequest(id, requestId, true,
            authentication.getName());
    }

    @PatchMapping("/{id}/join-requests/{requestId}/reject")
    public CommunityAccessResponse rejectJoinRequest(@PathVariable Long id,
        @PathVariable Long requestId, Authentication authentication) {
        return communityService.resolveJoinRequest(id, requestId, false,
            authentication.getName());
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityAccessResponse invite(@PathVariable Long id,
        @Valid @RequestBody CreateCommunityInvitationRequest request,
        Authentication authentication) {
        return communityService.invite(id, request.email(), authentication.getName());
    }

    @GetMapping("/invitations")
    public List<CommunityAccessResponse> pendingInvitations(
        Authentication authentication) {
        return communityService.pendingInvitations(authentication.getName());
    }

    @PatchMapping("/invitations/{invitationId}/accept")
    public CommunityAccessResponse acceptInvitation(
        @PathVariable Long invitationId, Authentication authentication) {
        return communityService.resolveInvitation(invitationId, true,
            authentication.getName());
    }

    @PatchMapping("/invitations/{invitationId}/reject")
    public CommunityAccessResponse rejectInvitation(
        @PathVariable Long invitationId, Authentication authentication) {
        return communityService.resolveInvitation(invitationId, false,
            authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunitySummaryResponse create(
        @Valid @RequestBody CreateCommunityRequest request,
        Authentication authentication
    ) {
        return communityService.create(request, authentication.getName());
    }
}
