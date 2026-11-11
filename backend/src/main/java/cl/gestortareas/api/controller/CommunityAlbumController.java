package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.gestortareas.api.service.CommunityAlbumService;
import cl.gestortareas.api.service.CommunityAlbumService.Album;
import cl.gestortareas.api.service.CommunityAlbumService.Image;
import cl.gestortareas.api.service.CommunityAlbumService.ImageComment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/albums")
public class CommunityAlbumController {
    private static final String COMMENTS = "/{albumId}/images/{imageId}/comments";

    public record CreateAlbum(@NotBlank @Size(max = 120) String title,
                              @NotNull @Size(max = 500) String description) {}
    public record CreateComment(@NotBlank @Size(max = 2000) String content) {}

    private final CommunityAlbumService albums;

    public CommunityAlbumController(CommunityAlbumService albums) {
        this.albums = albums;
    }

    @GetMapping
    public List<Album> findAll(@PathVariable Long groupId, Authentication auth) {
        return albums.findAll(groupId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long groupId, @Valid @RequestBody CreateAlbum request, Authentication auth) {
        albums.create(groupId, auth.getName(), request.title(), request.description());
    }

    @GetMapping("/{albumId}/images")
    public List<Image> images(@PathVariable Long groupId, @PathVariable Long albumId, Authentication auth) {
        return albums.images(groupId, albumId, auth.getName());
    }

    @PostMapping(value = "/{albumId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Image upload(@PathVariable Long groupId, @PathVariable Long albumId,
                        @RequestPart("file") MultipartFile file, Authentication auth) {
        return albums.upload(groupId, albumId, auth.getName(), file);
    }

    @GetMapping("/{albumId}/images/{imageId}")
    public ResponseEntity<byte[]> read(@PathVariable Long groupId, @PathVariable Long albumId,
                                      @PathVariable Long imageId, Authentication auth) {
        var image = albums.read(groupId, albumId, imageId, auth.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noStore()).body(image.data());
    }

    @GetMapping(COMMENTS)
    public List<ImageComment> comments(@PathVariable Long groupId, @PathVariable Long albumId,
                                       @PathVariable Long imageId, Authentication auth) {
        return albums.comments(groupId, albumId, imageId, auth.getName());
    }

    @PostMapping(COMMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageComment comment(@PathVariable Long groupId, @PathVariable Long albumId,
                                @PathVariable Long imageId, @Valid @RequestBody CreateComment request,
                                Authentication auth) {
        return albums.comment(groupId, albumId, imageId, auth.getName(), request.content());
    }

    @PutMapping(COMMENTS + "/{commentId}/like")
    public ImageComment like(@PathVariable Long groupId, @PathVariable Long albumId,
                             @PathVariable Long imageId, @PathVariable Long commentId, Authentication auth) {
        return albums.setLike(groupId, albumId, imageId, commentId, auth.getName(), true);
    }

    @DeleteMapping(COMMENTS + "/{commentId}/like")
    public ImageComment unlike(@PathVariable Long groupId, @PathVariable Long albumId,
                               @PathVariable Long imageId, @PathVariable Long commentId, Authentication auth) {
        return albums.setLike(groupId, albumId, imageId, commentId, auth.getName(), false);
    }
}
