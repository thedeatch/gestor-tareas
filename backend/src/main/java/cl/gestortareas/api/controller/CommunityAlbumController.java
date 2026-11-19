package cl.gestortareas.api.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.gestortareas.api.service.CommunityAlbumService;
import cl.gestortareas.api.service.CommunityAlbumService.Album;
import cl.gestortareas.api.service.CommunityAlbumService.Image;
import cl.gestortareas.api.service.CommunityAlbumService.ImagePage;
import cl.gestortareas.api.service.CommunityAlbumService.ImageComment;
import cl.gestortareas.api.service.CommunityAlbumService.ImageLikes;
import cl.gestortareas.api.service.CommunityAlbumService.ImageLiker;
import cl.gestortareas.api.service.CommunityAlbumService.ShareLink;
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
    public record PhotoDateRequest(LocalDate takenOn, Integer takenYear) {}

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
                        @RequestPart("file") MultipartFile file,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenOn,
                        @RequestParam(required = false) Integer takenYear, Authentication auth) {
        return albums.upload(groupId, albumId, auth.getName(), file, takenOn, takenYear);
    }

    @GetMapping("/{albumId}/images/page")
    public ImagePage imagePage(@PathVariable Long groupId, @PathVariable Long albumId,
                               @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return albums.imagePage(groupId, albumId, auth.getName(), page);
    }

    @GetMapping("/{albumId}/images/{imageId}/details")
    public Image image(@PathVariable Long groupId, @PathVariable Long albumId, @PathVariable Long imageId,
                       Authentication auth) {
        return albums.image(groupId, albumId, imageId, auth.getName());
    }

    @PatchMapping("/{albumId}/images/{imageId}/date")
    public Image updateDate(@PathVariable Long groupId, @PathVariable Long albumId,
                            @PathVariable Long imageId, @RequestBody PhotoDateRequest request,
                            Authentication auth) {
        return albums.updateDate(groupId, albumId, imageId, auth.getName(), request.takenOn(), request.takenYear());
    }

    @GetMapping("/{albumId}/images/{imageId}")
    public ResponseEntity<byte[]> read(@PathVariable Long groupId, @PathVariable Long albumId,
                                      @PathVariable Long imageId, Authentication auth) {
        var image = albums.read(groupId, albumId, imageId, auth.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noStore()).body(image.data());
    }

    @GetMapping("/{albumId}/images/{imageId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long groupId, @PathVariable Long albumId,
                                           @PathVariable Long imageId, Authentication auth) {
        var image = albums.thumbnail(groupId, albumId, imageId, auth.getName());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.noStore()).body(image.data());
    }

    @PutMapping("/{albumId}/images/{imageId}/like")
    public ImageLikes likeImage(@PathVariable Long groupId, @PathVariable Long albumId,
                                @PathVariable Long imageId, Authentication auth) {
        return albums.setImageLike(groupId, albumId, imageId, auth.getName(), true);
    }

    @DeleteMapping("/{albumId}/images/{imageId}/like")
    public ImageLikes unlikeImage(@PathVariable Long groupId, @PathVariable Long albumId,
                                  @PathVariable Long imageId, Authentication auth) {
        return albums.setImageLike(groupId, albumId, imageId, auth.getName(), false);
    }

    @GetMapping("/{albumId}/images/{imageId}/likes")
    public List<ImageLiker> imageLikers(@PathVariable Long groupId, @PathVariable Long albumId,
                                        @PathVariable Long imageId, Authentication auth) {
        return albums.imageLikers(groupId, albumId, imageId, auth.getName());
    }

    @PostMapping("/{albumId}/images/{imageId}/share")
    public ShareLink share(@PathVariable Long groupId, @PathVariable Long albumId,
                           @PathVariable Long imageId, Authentication auth) {
        return albums.createShareLink(groupId, albumId, imageId, auth.getName());
    }

    @DeleteMapping("/{albumId}/images/{imageId}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeShare(@PathVariable Long groupId, @PathVariable Long albumId,
                            @PathVariable Long imageId, Authentication auth) {
        albums.revokeShareLink(groupId, albumId, imageId, auth.getName());
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
