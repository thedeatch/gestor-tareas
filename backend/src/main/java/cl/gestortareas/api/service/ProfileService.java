package cl.gestortareas.api.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.ChangePasswordRequest;
import cl.gestortareas.api.dto.ProfileResponse;
import cl.gestortareas.api.dto.PublicProfileResponse;
import cl.gestortareas.api.dto.UpdateProfileRequest;
import cl.gestortareas.api.dto.UpdateUserPreferencesRequest;
import cl.gestortareas.api.dto.UserPreferencesResponse;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.FilePurpose;
import cl.gestortareas.api.model.StoredFile;
import cl.gestortareas.api.model.CommunityVisibility;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;

@Service
public class ProfileService {

    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final FileStorageService fileStorageService;
    private final CommunityMembershipRepository membershipRepository;

    public ProfileService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        FileStorageService fileStorageService,
        CommunityMembershipRepository membershipRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.fileStorageService = fileStorageService;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String authenticatedEmail) {
        AppUser user = findUser(authenticatedEmail);
        return new ProfileResponse(
            user.getName(),
            user.getEmail(),
            user.hasAvatar(),
            user.getRole(),
            user.getBio(),
            user.isPublicProfile(),
            user.isShowPublicCommunities()
        );
    }

    @Transactional
    public AuthResponse updateProfile(
        String authenticatedEmail,
        UpdateProfileRequest request
    ) {
        AppUser user = findUser(authenticatedEmail);
        requireCurrentPassword(user, request.currentPassword());

        String newEmail = normalizeEmail(request.email());
        boolean belongsToAnotherUser = userRepository
            .findByEmailIgnoreCase(newEmail)
            .filter(found -> !found.getId().equals(user.getId()))
            .isPresent();

        if (belongsToAnotherUser) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya está registrado"
            );
        }

        user.updateProfile(request.name().trim(), newEmail);
        user.updatePublicProfile(
            request.bio(),
            request.publicProfile() == null
                ? user.isPublicProfile()
                : request.publicProfile(),
            request.showPublicCommunities() == null
                ? user.isShowPublicCommunities()
                : request.showPublicCommunities()
        );
        return new AuthResponse(
            jwtService.generateToken(user),
            newEmail,
            user.getName(),
            user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {
        AppUser user = findPublicUser(userId);
        var communities = user.isShowPublicCommunities()
            ? membershipRepository.findAllByUserIdOrderByJoinedAtDesc(userId)
                .stream()
                .filter(membership -> membership.getCommunity().getVisibility()
                    == CommunityVisibility.PUBLIC)
                .map(membership -> new PublicProfileResponse.PublicCommunity(
                    membership.getCommunity().getId(),
                    membership.getCommunity().getName(),
                    membership.getCommunity().getSlug(),
                    membership.getRole()
                ))
                .toList()
            : java.util.List.<PublicProfileResponse.PublicCommunity>of();

        return new PublicProfileResponse(
            user.getId(),
            user.getName(),
            user.getBio(),
            user.hasAvatar(),
            user.getCreatedAt(),
            communities
        );
    }

    @Transactional(readOnly = true)
    public AvatarData getPublicAvatar(Long userId) {
        AppUser user = findPublicUser(userId);
        if (!user.hasAvatar()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El usuario no tiene una foto de perfil"
            );
        }
        if (user.getAvatarFile() != null) {
            StoredFile file = user.getAvatarFile();
            return new AvatarData(file.getData(), file.getContentType());
        }
        return new AvatarData(user.getAvatarData(), user.getAvatarContentType());
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences(String authenticatedEmail) {
        AppUser user = findUser(authenticatedEmail);
        return preferencesResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(
        String authenticatedEmail,
        UpdateUserPreferencesRequest request
    ) {
        AppUser user = findUser(authenticatedEmail);
        user.updatePreferences(request.theme(), request.reducedMotion());
        return preferencesResponse(user);
    }

    @Transactional
    public void changePassword(
        String authenticatedEmail,
        ChangePasswordRequest request
    ) {
        AppUser user = findUser(authenticatedEmail);
        requireCurrentPassword(user, request.currentPassword());

        if (passwordEncoder.matches(
            request.newPassword(),
            user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña nueva debe ser diferente"
            );
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user);
    }

    @Transactional(readOnly = true)
    public AvatarData getAvatar(String authenticatedEmail) {
        AppUser user = findUser(authenticatedEmail);
        if (!user.hasAvatar()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El usuario no tiene una foto de perfil"
            );
        }
        if (user.getAvatarFile() != null) {
            StoredFile file = user.getAvatarFile();
            return new AvatarData(file.getData(), file.getContentType());
        }
        return new AvatarData(
            user.getAvatarData(),
            user.getAvatarContentType()
        );
    }

    @Transactional
    public void updateAvatar(
        String authenticatedEmail,
        MultipartFile file
    ) {
        if (file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La imagen debe pesar como máximo 2 MB"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Utiliza una imagen JPEG, PNG o WebP"
            );
        }

        AppUser user = findUser(authenticatedEmail);
        StoredFile stored = fileStorageService.store(
            authenticatedEmail, file, FilePurpose.AVATAR,
            MAX_AVATAR_SIZE, ALLOWED_AVATAR_TYPES
        );
        StoredFile previous = user.updateAvatarFile(stored);
        if (previous != null) {
            fileStorageService.delete(previous);
        }
    }

    @Transactional
    public void removeAvatar(String authenticatedEmail) {
        AppUser user = findUser(authenticatedEmail);
        StoredFile previous = user.getAvatarFile();
        user.removeAvatar();
        if (previous != null) {
            fileStorageService.delete(previous);
        }
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no encontrado"
            ));
    }

    private AppUser findPublicUser(Long id) {
        return userRepository.findById(id)
            .filter(AppUser::isActive)
            .filter(AppUser::isPublicProfile)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Perfil público no disponible"
            ));
    }

    private void requireCurrentPassword(AppUser user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "La contraseña actual es incorrecta"
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserPreferencesResponse preferencesResponse(AppUser user) {
        return new UserPreferencesResponse(
            user.getThemePreference(),
            user.isReducedMotion()
        );
    }

    public record AvatarData(byte[] bytes, String contentType) {
    }
}
