package cl.gestortareas.api.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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
import cl.gestortareas.api.model.FilePurpose;
import cl.gestortareas.api.model.StoredFile;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityAccessRequestRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;

@Service
public class CommunityService {
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp");
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityAccessRequestRepository accessRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;
    private final UserBlockService blocks;
    private final FileStorageService fileStorageService;

    public CommunityService(CommunityRepository communityRepository,
                            CommunityMembershipRepository membershipRepository,
                            CommunityAccessRequestRepository accessRepository,
                            AppUserRepository userRepository,
                            NotificationService notificationService, UserBlockService blocks,
                            FileStorageService fileStorageService) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.accessRepository = accessRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blocks = blocks;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryResponse> findMine(String email) {
        return membershipRepository
            .findAllByUserEmailIgnoreCaseOrderByJoinedAtDesc(email)
            .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public CommunityDetailResponse findById(Long id, String email) {
        return toDetail(requireMembership(id, email));
    }

    @Transactional
    public CommunityDetailResponse updateGuidelines(Long id, CommunityGuidelinesRequest request,
                                                    String email) {
        var membership = requireMembership(id, email);
        if (membership.getRole() != CommunityRole.OWNER
            && membership.getRole() != CommunityRole.COMMUNITY_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permisos para editar la descripción y las reglas");
        }
        membership.getCommunity().updateGuidelines(request.extendedDescription().strip(),
            request.rules().strip());
        return toDetail(membership);
    }

    CommunityMembership requireMembership(Long id, String email) {
        return membershipRepository.findByCommunityIdAndUserEmailIgnoreCase(id, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
    }

    private CommunityDetailResponse toDetail(CommunityMembership membership) {
        Community community = membership.getCommunity();
        List<CommunityMemberResponse> members = membershipRepository
            .findAllByCommunityIdOrderByJoinedAtAsc(community.getId())
            .stream().map(this::toMember).toList();
        return new CommunityDetailResponse(
            community.getId(), community.getName(), community.getSlug(),
            community.getDescription(), community.getExtendedDescription(), community.getRules(),
            community.hasImage(),
            community.getVisibility(), membership.getRole(), community.getCreatedAt(), members);
    }

    @Transactional(readOnly = true)
    public ImageData getImage(Long id, String email) {
        AppUser viewer = requireUser(email);
        Community community = communityRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        boolean isMember = membershipRepository
            .existsByCommunityIdAndUserId(id, viewer.getId());
        boolean hasInvitation = accessRepository
            .findByCommunityIdAndUserIdAndType(id, viewer.getId(),
                CommunityAccessType.INVITATION)
            .filter(invitation -> invitation.getStatus()
                == CommunityAccessStatus.PENDING)
            .isPresent();
        boolean isDiscoverable = community.getVisibility()
            != CommunityVisibility.HIDDEN;
        if (!isMember && !hasInvitation && !isDiscoverable) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Grupo no encontrado");
        }
        if (!community.hasImage()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El grupo no tiene una imagen");
        }
        StoredFile image = community.getImageFile();
        return new ImageData(fileStorageService.readBytes(image), image.getContentType());
    }

    @Transactional
    public void updateImage(Long id, MultipartFile file, String email) {
        CommunityMembership membership = requireImageManager(id, email);
        if (file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La imagen debe pesar como máximo 2 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Utiliza una imagen JPEG, PNG o WebP");
        }
        StoredFile stored = fileStorageService.store(
            email, file, FilePurpose.COMMUNITY_IMAGE, MAX_IMAGE_SIZE, ALLOWED_IMAGE_TYPES);
        StoredFile previous = membership.getCommunity().updateImage(stored);
        if (previous != null) fileStorageService.delete(previous);
    }

    @Transactional
    public void removeImage(Long id, String email) {
        StoredFile previous = requireImageManager(id, email).getCommunity().removeImage();
        if (previous != null) fileStorageService.delete(previous);
    }

    private CommunityMembership requireImageManager(Long id, String email) {
        CommunityMembership membership = requireMembership(id, email);
        if (membership.getRole() != CommunityRole.OWNER
            && membership.getRole() != CommunityRole.COMMUNITY_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permisos para editar la imagen del grupo");
        }
        return membership;
    }

    public record ImageData(byte[] bytes, String contentType) {}

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
                group.getDescription(), group.hasImage(),
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
        boolean hasPendingInvitation = accessRepository
            .findByCommunityIdAndUserIdAndType(group.getId(), requester.getId(),
                CommunityAccessType.INVITATION)
            .filter(invitation -> invitation.getStatus()
                == CommunityAccessStatus.PENDING)
            .isPresent();
        if (hasPendingInvitation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya tienes una invitación pendiente para este grupo");
        }
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
        requireInvitationAllowed(manager, invited);
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
            "/grupos?invitacion=" + invitation.getId());
        return toAccessResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<CommunityAccessResponse> pendingInvitations(String email) {
        requireUser(email);
        return accessRepository
            .findAllByUserEmailIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(
                email, CommunityAccessType.INVITATION,
                CommunityAccessStatus.PENDING)
            .stream().filter(invitation -> !blocks.between(
                invitation.getUser().getId(), invitation.getCreatedBy().getId()))
            .map(this::toAccessResponse).toList();
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
        if (accept) requireInvitationAllowed(invitation.getCreatedBy(), invited);
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
            community.getDescription(), community.hasImage(), community.getVisibility(),
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
            request.getCommunity().hasImage(),
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

    CommunityMembership requireManager(Long communityId, String email) {
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

    private void requireInvitationAllowed(AppUser sender, AppUser recipient) {
        if (blocks.between(sender.getId(), recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La invitación no está disponible");
        }
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
