package cl.gestortareas.api.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.gestortareas.api.dto.StoredFileResponse;
import cl.gestortareas.api.model.StoredFile;
import cl.gestortareas.api.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileResponse upload(
        @AuthenticationPrincipal Jwt jwt,
        @RequestPart("file") MultipartFile file
    ) {
        return fileStorageService.storeAttachment(jwt.getSubject(), file);
    }

    @GetMapping
    public List<StoredFileResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return fileStorageService.list(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        StoredFile file = fileStorageService.getOwned(jwt.getSubject(), id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build().toString())
            .body(fileStorageService.readBytes(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        fileStorageService.deleteOwned(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
