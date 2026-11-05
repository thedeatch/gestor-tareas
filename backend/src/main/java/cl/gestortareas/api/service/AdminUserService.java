package cl.gestortareas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.AdminUserResponse;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.AdminAuditAction;
import cl.gestortareas.api.model.UserRole;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final AdminAuditService auditService;
    private final NotificationService notificationService;

    public AdminUserService(
        AppUserRepository userRepository,
        AdminAuditService auditService,
        NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findUsers(
        String search,
        int page,
        int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String normalizedSearch = search == null ? "" : search.trim();

        return userRepository
            .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                normalizedSearch,
                normalizedSearch,
                pageable
            )
            .map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse updateRole(
        String authenticatedEmail,
        Long userId,
        UserRole newRole,
        String reason
    ) {
        AppUser actor = userRepository
            .findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sesión no válida"
            ));
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));

        boolean isCurrentUser = user.getEmail()
            .equalsIgnoreCase(authenticatedEmail);
        if (isCurrentUser && newRole != UserRole.ADMIN) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No puedes quitar tu propio rol de administrador"
            );
        }

        boolean removesLastAdmin = user.getRole() == UserRole.ADMIN
            && newRole != UserRole.ADMIN
            && userRepository.countByRole(UserRole.ADMIN) <= 1;
        if (removesLastAdmin) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Debe existir al menos un administrador"
            );
        }

        UserRole previousRole = user.getRole();
        if (previousRole == newRole) {
            return toResponse(user);
        }

        String normalizedReason = requireReason(reason);

        user.changeRole(newRole);
        auditService.record(
            authenticatedEmail,
            user,
            AdminAuditAction.ROLE_CHANGED,
            previousRole.name(),
            newRole.name(),
            normalizedReason
        );
        notificationService.create(
            user,
            actor,
            NotificationType.MODERATION,
            "Tu rol fue actualizado",
            "Tu rol cambió de " + roleLabel(previousRole)
                + " a " + roleLabel(newRole) + ".",
            "/perfil"
        );
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(
        String authenticatedEmail,
        Long userId,
        boolean active,
        String reason
    ) {
        AppUser actor = userRepository.findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sesión no válida"
            ));
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));

        boolean isCurrentUser = user.getEmail()
            .equalsIgnoreCase(authenticatedEmail);
        if (isCurrentUser && !active) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No puedes suspender tu propia cuenta"
            );
        }

        if (actor.getRole() == UserRole.MODERATOR
            && user.getRole() != UserRole.USER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Un moderador solo puede administrar cuentas de usuario"
            );
        }

        boolean previousActive = user.isActive();
        if (previousActive == active) {
            return toResponse(user);
        }

        String normalizedReason = requireReason(reason);

        user.changeActiveStatus(active);
        auditService.record(
            authenticatedEmail,
            user,
            active
                ? AdminAuditAction.ACCOUNT_ACTIVATED
                : AdminAuditAction.ACCOUNT_SUSPENDED,
            previousActive ? "ACTIVE" : "SUSPENDED",
            active ? "ACTIVE" : "SUSPENDED",
            normalizedReason
        );
        notificationService.create(
            user,
            actor,
            NotificationType.MODERATION,
            active ? "Tu cuenta fue reactivada" : "Tu cuenta fue suspendida",
            active
                ? "Ya puedes volver a utilizar tu cuenta. Motivo: "
                    + normalizedReason
                : "Se suspendió el acceso a tu cuenta. Motivo: "
                    + normalizedReason,
            "/perfil"
        );
        return toResponse(user);
    }

    private String requireReason(String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes indicar el motivo de la acción administrativa"
            );
        }
        return normalizedReason;
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case USER -> "Usuario";
            case MODERATOR -> "Moderador";
            case ADMIN -> "Administrador";
        };
    }

    private AdminUserResponse toResponse(AppUser user) {
        return new AdminUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()
        );
    }
}
