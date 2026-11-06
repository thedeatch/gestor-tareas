package cl.gestortareas.api.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.*;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.CommunityAccessRequest;
import cl.gestortareas.api.model.CommunityAccessStatus;
import cl.gestortareas.api.model.CommunityAccessType;
import cl.gestortareas.api.model.Community;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.model.CommunityVisibility;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityAccessRequestRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;

@Service
public class CommunityService {
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityAccessRequestRepository accessRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;

    public CommunityService(CommunityRepository communityRepository,
                            CommunityMembershipRepository membershipRepository,
                            CommunityAccessRequestRepository accessRepository,
                            AppUserRepository userRepository,
                            NotificationService notificationService) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.accessRepository = accessRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryResponse> findMine(String email) {
        return membershipRepository
            .findAllByUserEmailIgnoreCaseOrderByJoinedAtDesc(email)
            .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public CommunityDetailResponse findById(Long id, String email) {
        CommunityMembership membership = membershipRepository
            .findByCommunityIdAndUserEmailIgnoreCase(id, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        Community community = membership.getCommunity();
        List<CommunityMemberResponse> members = membershipRepository
            .findAllByCommunityIdOrderByJoinedAtAsc(id)
            .stream().map(this::toMember).toList();
        return new CommunityDetailResponse(
            community.getId(), community.getName(), community.getSlug(),
            community.getDescription(), community.getVisibility(),
            membership.getRole(), community.getCreatedAt(), members);
    }

    @Transactional
    public CommunitySummaryResponse create(CreateCommunityRequest request,
                                           String email) {
        AppUser owner = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
        String name = request.name().trim();
        Community community = communityRepository.save(new Community(
            name, uniqueSlug(name), request.description().trim(),
            request.visibility(), owner));
        return toSummary(membershipRepository.save(
            new CommunityMembership(community, owner, CommunityRole.OWNER)));
    }

    @Transactional(readOnly = true)
    public List<CommunityDiscoveryResponse> discover(String email) {
        AppUser user = requireUser(email);
        return communityRepository
            .findAllByVisibilityInOrderByCreatedAtDesc(List.of(
                CommunityVisibility.PUBLIC, CommunityVisibility.PRIVATE))
            .stream()
            .filter(group -> !membershipRepository
                .existsByCommunityIdAndUserId(group.getId(), user.getId()))
            .map(group -> new CommunityDiscoveryResponse(
                group.getId(), group.getName(), group.getSlug(),
                group.getDescription(),
                membershipRepository.countByCommunityId(group.getId()),
                accessRepository.findByCommunityIdAndUserIdAndType(
                    group.getId(), user.getId(), CommunityAccessType.JOIN_REQUEST)
                    .map(CommunityAccessRequest::getStatus).orElse(null)))
            .toList();
    }

    @Transactional
    public CommunityAccessResponse requestToJoin(Long communityId,
                                                 String email) {
        AppUser requester = requireUser(email);
        Community group = visibleGroup(communityId);
        ensureNotMember(group.getId(), requester.getId());
        CommunityAccessRequest request = accessRepository
            .findByCommunityIdAndUserIdAndType(group.getId(), requester.getId(),
                CommunityAccessType.JOIN_REQUEST)
            .map(existing -> { existing.reopen(requester); return existing; })
            .orElseGet(() -> new CommunityAccessRequest(group, requester,
                requester, CommunityAccessType.JOIN_REQUEST));
        request = accessRepository.save(request);
        notifyManagers(group, requester, "Nueva solicitud de ingreso",
            requester.getName() + " quiere unirse a " + group.getName() + ".");
        return toAccessResponse(request);
    }

    @Transactional
    public CommunityAccessResponse cancelJoinRequest(Long communityId,
                                                      String email) {
        AppUser requester = requireUser(email);
        CommunityAccessRequest request = accessRepository
            .findByCommunityIdAndUserIdAndType(communityId, requester.getId(),
                CommunityAccessType.JOIN_REQUEST)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Solicitud no encontrada"));
        requirePending(request);
        request.resolve(CommunityAccessStatus.CANCELLED, requester);
        return toAccessResponse(request);
    }

    @Transactional(readOnly = true)
    public List<CommunityAccessResponse> pendingJoinRequests(Long communityId,
                                                             String email) {
        requireManager(communityId, email);
        return accessRepository
            .findAllByCommunityIdAndTypeAndStatusOrderByCreatedAtAsc(
                communityId, CommunityAccessType.JOIN_REQUEST,
                CommunityAccessStatus.PENDING)
            .stream().map(this::toAccessResponse).toList();
    }

    @Transactional
    public CommunityAccessResponse resolveJoinRequest(Long communityId,
        Long requestId, boolean approve, String email) {
        AppUser manager = requireManager(communityId, email).getUser();
        CommunityAccessRequest request = accessRepository
            .findByIdAndType(requestId, CommunityAccessType.JOIN_REQUEST)
            .filter(item -> item.getCommunity().getId().equals(communityId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Solicitud no encontrada"));
        requirePending(request);
        CommunityAccessStatus status = approve
            ? CommunityAccessStatus.APPROVED : CommunityAccessStatus.REJECTED;
        request.resolve(status, manager);
        if (approve) addMemberIfMissing(request.getCommunity(), request.getUser());
        notificationService.create(request.getUser(), manager,
            NotificationType.INVITATION,
            approve ? "Solicitud aprobada" : "Solicitud rechazada",
            approve
                ? "Ya formas parte de " + request.getCommunity().getName() + "."
                : "Tu solicitud para " + request.getCommunity().getName()
                    + " fue rechazada.",
            approve ? "/grupos/" + communityId : "/grupos");
        return toAccessResponse(request);
    }

    @Transactional
    public CommunityAccessResponse invite(Long communityId, String invitedEmail,
                                          String email) {
        CommunityMembership managerMembership = requireManager(communityId, email);
        AppUser manager = managerMembership.getUser();
        AppUser invited = userRepository.findByEmailIgnoreCase(invitedEmail.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe una cuenta con ese correo"));
        ensureNotMember(communityId, invited.getId());
        Community group = managerMembership.getCommunity();
        CommunityAccessRequest invitation = accessRepository
            .findByCommunityIdAndUserIdAndType(communityId, invited.getId(),
                CommunityAccessType.INVITATION)
            .map(existing -> { existing.reopen(manager); return existing; })
            .orElseGet(() -> new CommunityAccessRequest(group, invited, manager,
                CommunityAccessType.INVITATION));
        invitation = accessRepository.save(invitation);
        notificationService.create(invited, manager, NotificationType.INVITATION,
            "Invitación a un grupo",
            manager.getName() + " te invitó a " + group.getName() + ".",
            "/grupos");
        return toAccessResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<CommunityAccessResponse> pendingInvitations(String email) {
        requireUser(email);
        return accessRepository
            .findAllByUserEmailIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(
                email, CommunityAccessType.INVITATION,
                CommunityAccessStatus.PENDING)
            .stream().map(this::toAccessResponse).toList();
    }

    @Transactional
    public CommunityAccessResponse resolveInvitation(Long invitationId,
        boolean accept, String email) {
        AppUser invited = requireUser(email);
        CommunityAccessRequest invitation = accessRepository
            .findByIdAndType(invitationId, CommunityAccessType.INVITATION)
            .filter(item -> item.getUser().getId().equals(invited.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Invitación no encontrada"));
        requirePending(invitation);
        invitation.resolve(accept ? CommunityAccessStatus.APPROVED
            : CommunityAccessStatus.REJECTED, invited);
        if (accept) addMemberIfMissing(invitation.getCommunity(), invited);
        notificationService.create(invitation.getCreatedBy(), invited,
            NotificationType.INVITATION,
            accept ? "Invitación aceptada" : "Invitación rechazada",
            invited.getName() + (accept ? " aceptó" : " rechazó")
                + " la invitación a " + invitation.getCommunity().getName() + ".",
            "/grupos/" + invitation.getCommunity().getId());
        return toAccessResponse(invitation);
    }

    @Transactional
    public CommunityMemberResponse updateMemberRole(
        Long communityId,
        Long userId,
        CommunityRole newRole,
        String email
    ) {
        CommunityMembership actorMembership = membershipRepository
            .findByCommunityIdAndUserEmailIgnoreCase(communityId, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        CommunityMembership targetMembership = membershipRepository
            .findByCommunityIdAndUserId(communityId, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Miembro no encontrado"));

        validateRoleChange(actorMembership, targetMembership, newRole);
        CommunityRole previousRole = targetMembership.getRole();
        if (previousRole == newRole) return toMember(targetMembership);

        targetMembership.changeRole(newRole);
        notificationService.create(targetMembership.getUser(),
            actorMembership.getUser(), NotificationType.MODERATION,
            "Tu rol en el grupo cambió",
            "Ahora eres " + roleLabel(newRole) + " en "
                + targetMembership.getCommunity().getName() + ".",
            "/grupos/" + communityId);
        return toMember(targetMembership);
    }

    private CommunitySummaryResponse toSummary(CommunityMembership membership) {
        Community community = membership.getCommunity();
        return new CommunitySummaryResponse(
            community.getId(), community.getName(), community.getSlug(),
            community.getDescription(), community.getVisibility(),
            membership.getRole(),
            membershipRepository.countByCommunityId(community.getId()),
            community.getCreatedAt());
    }

    private CommunityMemberResponse toMember(CommunityMembership membership) {
        AppUser user = membership.getUser();
        return new CommunityMemberResponse(user.getId(), user.getName(),
            user.getEmail(), membership.getRole(), membership.getJoinedAt());
    }

    private CommunityAccessResponse toAccessResponse(
        CommunityAccessRequest request) {
        return new CommunityAccessResponse(request.getId(),
            request.getCommunity().getId(), request.getCommunity().getName(),
            request.getUser().getId(), request.getUser().getName(),
            request.getUser().getEmail(), request.getType(), request.getStatus(),
            request.getCreatedAt());
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private Community visibleGroup(Long id) {
        return communityRepository.findById(id)
            .filter(group -> group.getVisibility() != CommunityVisibility.HIDDEN)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
    }

    private CommunityMembership requireManager(Long communityId, String email) {
        CommunityMembership membership = membershipRepository
            .findByCommunityIdAndUserEmailIgnoreCase(communityId, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        if (!Set.of(CommunityRole.OWNER, CommunityRole.COMMUNITY_ADMIN,
            CommunityRole.COMMUNITY_MODERATOR).contains(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permisos para administrar este grupo");
        }
        return membership;
    }

    private void validateRoleChange(CommunityMembership actor,
                                    CommunityMembership target,
                                    CommunityRole newRole) {
        CommunityRole actorRole = actor.getRole();
        CommunityRole targetRole = target.getRole();
        if (newRole == CommunityRole.OWNER || targetRole == CommunityRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "El rol de propietario no puede modificarse aquí");
        }
        if (actorRole == CommunityRole.OWNER) return;
        boolean adminCanChange = actorRole == CommunityRole.COMMUNITY_ADMIN
            && targetRole != CommunityRole.COMMUNITY_ADMIN
            && Set.of(CommunityRole.COMMUNITY_MODERATOR, CommunityRole.MEMBER)
                .contains(newRole);
        if (!adminCanChange) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permisos para cambiar este rol");
        }
    }

    private String roleLabel(CommunityRole role) {
        return switch (role) {
            case OWNER -> "propietario";
            case COMMUNITY_ADMIN -> "administrador";
            case COMMUNITY_MODERATOR -> "moderador";
            case MEMBER -> "miembro";
        };
    }

    private void ensureNotMember(Long communityId, Long userId) {
        if (membershipRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El usuario ya pertenece al grupo");
        }
    }

    private void requirePending(CommunityAccessRequest request) {
        if (request.getStatus() != CommunityAccessStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La solicitud ya fue respondida");
        }
    }

    private void addMemberIfMissing(Community group, AppUser user) {
        if (!membershipRepository.existsByCommunityIdAndUserId(
            group.getId(), user.getId())) {
            membershipRepository.save(new CommunityMembership(
                group, user, CommunityRole.MEMBER));
        }
    }

    private void notifyManagers(Community group, AppUser requester,
                                String title, String message) {
        membershipRepository.findAllByCommunityIdAndRoleIn(group.getId(), List.of(
            CommunityRole.OWNER, CommunityRole.COMMUNITY_ADMIN,
            CommunityRole.COMMUNITY_MODERATOR))
            .forEach(membership -> notificationService.create(
                membership.getUser(), requester, NotificationType.INVITATION,
                title, message, "/grupos/" + group.getId()));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "grupo";
        if (base.length() > 105) {
            base = base.substring(0, 105).replaceAll("-$", "");
        }
        String candidate = base;
        int suffix = 2;
        while (communityRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
