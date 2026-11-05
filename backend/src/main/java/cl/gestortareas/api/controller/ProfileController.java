package cl.gestortareas.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.ChangePasswordRequest;
import cl.gestortareas.api.dto.ProfileResponse;
import cl.gestortareas.api.dto.UpdateProfileRequest;
import cl.gestortareas.api.dto.UpdateUserPreferencesRequest;
import cl.gestortareas.api.dto.UserPreferencesResponse;
import cl.gestortareas.api.service.ProfileService;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt.getSubject());
    }

    @PatchMapping
    public AuthResponse updateProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(jwt.getSubject(), request);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar(@AuthenticationPrincipal Jwt jwt) {
        ProfileService.AvatarData avatar = profileService.getAvatar(
            jwt.getSubject()
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(avatar.contentType()))
            .cacheControl(CacheControl.noStore())
            .body(avatar.bytes());
    }

    @PostMapping(
        path = "/avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> updateAvatar(
        @AuthenticationPrincipal Jwt jwt,
        @RequestPart("file") MultipartFile file
    ) {
        profileService.updateAvatar(jwt.getSubject(), file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> removeAvatar(
        @AuthenticationPrincipal Jwt jwt
    ) {
        profileService.removeAvatar(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public UserPreferencesResponse getPreferences(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return profileService.getPreferences(jwt.getSubject());
    }

    @PutMapping("/preferences")
    public UserPreferencesResponse updatePreferences(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        return profileService.updatePreferences(jwt.getSubject(), request);
    }
}
