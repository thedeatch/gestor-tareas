package cl.gestortareas.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityAlbumService;
import cl.gestortareas.api.service.CommunityAlbumService.PublicImage;

@RestController
@RequestMapping("/api/public/gallery-images")
public class PublicGalleryImageController {
    private final CommunityAlbumService albums;

    public PublicGalleryImageController(CommunityAlbumService albums) {
        this.albums = albums;
    }

    @GetMapping("/{token}")
    public PublicImage find(@PathVariable String token) {
        return albums.publicImage(token);
    }

    @GetMapping("/{token}/content")
    public ResponseEntity<byte[]> content(@PathVariable String token) {
        var image = albums.publicImageData(token);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noStore())
            .body(image.data());
    }
}
