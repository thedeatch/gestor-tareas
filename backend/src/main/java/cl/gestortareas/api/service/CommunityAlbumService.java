package cl.gestortareas.api.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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
    private static final String SELECT_COMMENTS = """
        SELECT c.id, c.author_user_id AS author_id, u.name AS author_name,
               (u.avatar_file_id IS NOT NULL OR COALESCE(OCTET_LENGTH(u.avatar_data), 0) > 0) AS author_has_avatar,
               c.content, c.created_at,
               (SELECT COUNT(*) FROM community_image_comment_likes r WHERE r.comment_id = c.id) AS like_count,
               EXISTS (SELECT 1 FROM community_image_comment_likes r
                       WHERE r.comment_id = c.id AND r.user_id = :userId) AS liked
        FROM community_image_comments c JOIN users u ON u.id = c.author_user_id
        WHERE c.image_id = :imageId
        """;

    public record Album(Long id, String title, String description, long imageCount, Long coverImageId) {}
    public record Image(Long id, String name, long commentCount, long likeCount, boolean liked) {}
    public record ImageLikes(long likeCount, boolean liked) {}
    public record ImageData(String contentType, byte[] data) {}
    public record ImageComment(Long id, Long authorId, String authorName, boolean authorHasAvatar,
                               String content, OffsetDateTime createdAt, long likeCount, boolean liked) {}

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
            SELECT a.id, a.title, a.description, COUNT(i.file_id) AS image_count,
                   MAX(i.file_id) AS cover_image_id
            FROM community_albums a
            LEFT JOIN community_album_images i ON i.album_id = a.id
            WHERE a.community_id = :groupId
            GROUP BY a.id, a.title, a.description
            ORDER BY a.id DESC
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
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        requireAlbum(groupId, albumId);
        return jdbc.sql("""
            SELECT f.id, f.original_name AS name,
                   (SELECT COUNT(*) FROM community_image_comments c WHERE c.image_id = f.id) AS comment_count,
                   (SELECT COUNT(*) FROM community_image_likes r WHERE r.image_id = f.id) AS like_count,
                   EXISTS (SELECT 1 FROM community_image_likes r
                           WHERE r.image_id = f.id AND r.user_id = :userId) AS liked
            FROM community_album_images i JOIN stored_files f ON f.id = i.file_id
            WHERE i.album_id = :albumId ORDER BY f.id DESC
            """).param("albumId", albumId).param("userId", userId).query(Image.class).list();
    }

    public Image upload(Long groupId, Long albumId, String email, MultipartFile file) {
        communities.requireManager(groupId, email);
        requireAlbum(groupId, albumId);
        validateHeader(file);
        var saved = storage.store(email, file, FilePurpose.COMMUNITY_IMAGE, MAX_IMAGE_SIZE, IMAGE_TYPES);
        jdbc.sql("""
            INSERT INTO community_album_images (album_id, file_id) VALUES (:albumId, :fileId)
            """).param("albumId", albumId).param("fileId", saved.getId()).update();
        return new Image(saved.getId(), saved.getOriginalName(), 0, 0, false);
    }

    @Transactional(readOnly = true)
    public ImageData read(Long groupId, Long albumId, Long imageId, String email) {
        communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT f.content_type, f.data, f.object_key
            FROM community_album_images i
            JOIN community_albums a ON a.id = i.album_id
            JOIN stored_files f ON f.id = i.file_id
            WHERE a.community_id = :groupId AND a.id = :albumId AND f.id = :imageId
            """).param("groupId", groupId).param("albumId", albumId).param("imageId", imageId)
            .query((rs, row) -> new ImageData(rs.getString("content_type"),
                storage.readBytes(rs.getString("object_key"), rs.getBytes("data"))))
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<ImageComment> comments(Long groupId, Long albumId, Long imageId, String email) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        return jdbc.sql(SELECT_COMMENTS + " ORDER BY c.id")
            .param("userId", userId).param("imageId", imageId).query(ImageComment.class).list();
    }

    public ImageComment comment(Long groupId, Long albumId, Long imageId, String email, String content) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        var key = new GeneratedKeyHolder();
        jdbc.sql("""
            INSERT INTO community_image_comments (image_id, author_user_id, content)
            VALUES (:imageId, :userId, :content)
            """).param("imageId", imageId).param("userId", userId).param("content", content.strip())
            .update(key, "id");
        return findComment(imageId, key.getKeyAs(Long.class), userId);
    }

    public ImageComment setLike(Long groupId, Long albumId, Long imageId, Long commentId,
                                String email, boolean liked) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        jdbc.sql("SELECT id FROM community_image_comments WHERE id = :commentId AND image_id = :imageId FOR UPDATE")
            .param("commentId", commentId).param("imageId", imageId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));
        String sql = liked ? """
            INSERT INTO community_image_comment_likes (comment_id, user_id)
            SELECT :commentId, :userId WHERE NOT EXISTS (
                SELECT 1 FROM community_image_comment_likes WHERE comment_id = :commentId AND user_id = :userId
            )
            """ : "DELETE FROM community_image_comment_likes WHERE comment_id = :commentId AND user_id = :userId";
        jdbc.sql(sql).param("commentId", commentId).param("userId", userId).update();
        return findComment(imageId, commentId, userId);
    }

    public ImageLikes setImageLike(Long groupId, Long albumId, Long imageId, String email, boolean liked) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        jdbc.sql("SELECT file_id FROM community_album_images WHERE file_id = :imageId FOR UPDATE")
            .param("imageId", imageId).query(Long.class).single();
        String sql = liked ? """
            INSERT INTO community_image_likes (image_id, user_id)
            SELECT :imageId, :userId WHERE NOT EXISTS (
                SELECT 1 FROM community_image_likes WHERE image_id = :imageId AND user_id = :userId
            )
            """ : "DELETE FROM community_image_likes WHERE image_id = :imageId AND user_id = :userId";
        jdbc.sql(sql).param("imageId", imageId).param("userId", userId).update();
        return jdbc.sql("""
            SELECT COUNT(*) AS like_count,
                   EXISTS (SELECT 1 FROM community_image_likes WHERE image_id = :imageId AND user_id = :userId) AS liked
            FROM community_image_likes WHERE image_id = :imageId
            """).param("imageId", imageId).param("userId", userId).query(ImageLikes.class).single();
    }

    private ImageComment findComment(Long imageId, Long commentId, Long userId) {
        return jdbc.sql(SELECT_COMMENTS + " AND c.id = :commentId")
            .param("userId", userId).param("imageId", imageId).param("commentId", commentId)
            .query(ImageComment.class).single();
    }

    private Long requireImageMember(Long groupId, Long albumId, Long imageId, String email) {
        var member = communities.requireMembership(groupId, email);
        jdbc.sql("""
            SELECT i.file_id FROM community_album_images i JOIN community_albums a ON a.id = i.album_id
            WHERE a.community_id = :groupId AND a.id = :albumId AND i.file_id = :imageId
            """).param("groupId", groupId).param("albumId", albumId).param("imageId", imageId)
            .query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        return member.getUser().getId();
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
