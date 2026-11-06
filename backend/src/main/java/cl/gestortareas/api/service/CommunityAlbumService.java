package cl.gestortareas.api.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.FilePurpose;

@Service
@Transactional
public class CommunityAlbumService {
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] JPEG_HEADER = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};

    public record Album(Long id, String title, String description) {}
    public record Image(Long id, String name) {}
    public record ImageData(String contentType, byte[] data) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final FileStorageService storage;

    public CommunityAlbumService(JdbcClient jdbc, CommunityService communities, FileStorageService storage) {
        this.jdbc = jdbc;
        this.communities = communities;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<Album> findAll(Long groupId, String email) {
        communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT id, title, description FROM community_albums
            WHERE community_id = :groupId ORDER BY id DESC
            """).param("groupId", groupId).query(Album.class).list();
    }

    public void create(Long groupId, String email, String title, String description) {
        communities.requireManager(groupId, email);
        jdbc.sql("""
            INSERT INTO community_albums (community_id, title, description)
            VALUES (:groupId, :title, :description)
            """).param("groupId", groupId).param("title", title.strip())
            .param("description", description.strip()).update();
    }

    @Transactional(readOnly = true)
    public List<Image> images(Long groupId, Long albumId, String email) {
        communities.requireMembership(groupId, email);
        requireAlbum(groupId, albumId);
        return jdbc.sql("""
            SELECT f.id, f.original_name AS name
            FROM community_album_images i JOIN stored_files f ON f.id = i.file_id
            WHERE i.album_id = :albumId ORDER BY f.id DESC
            """).param("albumId", albumId).query(Image.class).list();
    }

    public Image upload(Long groupId, Long albumId, String email, MultipartFile file) {
        communities.requireManager(groupId, email);
        requireAlbum(groupId, albumId);
        validateHeader(file);
        var saved = storage.store(email, file, FilePurpose.COMMUNITY_IMAGE, MAX_IMAGE_SIZE, IMAGE_TYPES);
        jdbc.sql("""
            INSERT INTO community_album_images (album_id, file_id) VALUES (:albumId, :fileId)
            """).param("albumId", albumId).param("fileId", saved.getId()).update();
        return new Image(saved.getId(), saved.getOriginalName());
    }

    @Transactional(readOnly = true)
    public ImageData read(Long groupId, Long albumId, Long imageId, String email) {
        communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT f.content_type, f.data
            FROM community_album_images i
            JOIN community_albums a ON a.id = i.album_id
            JOIN stored_files f ON f.id = i.file_id
            WHERE a.community_id = :groupId AND a.id = :albumId AND f.id = :imageId
            """).param("groupId", groupId).param("albumId", albumId).param("imageId", imageId)
            .query((rs, row) -> new ImageData(rs.getString("content_type"), rs.getBytes("data")))
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
    }

    private void requireAlbum(Long groupId, Long albumId) {
        jdbc.sql("SELECT id FROM community_albums WHERE community_id = :groupId AND id = :albumId")
            .param("groupId", groupId).param("albumId", albumId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Álbum no encontrado"));
    }

    private void validateHeader(MultipartFile file) {
        byte[] signature = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case "image/jpeg" -> JPEG_HEADER;
            case "image/png" -> PNG_HEADER;
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Selecciona una imagen JPEG o PNG");
        };
        try (var stream = file.getInputStream()) {
            if (!Arrays.equals(signature, stream.readNBytes(signature.length))) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "El archivo no corresponde al formato de imagen indicado");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fue posible leer la imagen", exception);
        }
    }
}