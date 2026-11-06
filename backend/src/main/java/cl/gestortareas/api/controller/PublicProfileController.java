package cl.gestortareas.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.PublicProfileResponse;
import cl.gestortareas.api.service.ProfileService;

@RestController
@RequestMapping("/api/users")
public class PublicProfileController {

    private final ProfileService profileService;

    public PublicProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{id}/profile")
    public PublicProfileResponse getProfile(@PathVariable Long id) {
        return profileService.getPublicProfile(id);
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        ProfileService.AvatarData avatar = profileService.getPublicAvatar(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(avatar.contentType()))
            .cacheControl(CacheControl.noStore())
            .body(avatar.bytes());
    }
}
