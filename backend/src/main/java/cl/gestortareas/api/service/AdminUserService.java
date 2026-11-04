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
import cl.gestortareas.api.model.UserRole;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;

    public AdminUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
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
        UserRole newRole
    ) {
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

        user.changeRole(newRole);
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(
        String authenticatedEmail,
        Long userId,
        boolean active
    ) {
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

        user.changeActiveStatus(active);
        return toResponse(user);
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
