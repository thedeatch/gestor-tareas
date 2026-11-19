package cl.gestortareas.api.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.FilePurpose;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.service.CommunityMemoryService.PhotoDate;

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
    public record Image(Long id, String name, long commentCount, long likeCount, boolean liked, PhotoDate date) {}
    public record ImageLikes(long likeCount, boolean liked) {}
    public record ImageLiker(Long userId, String name, boolean hasAvatar) {}
    public record ImageData(String contentType, byte[] data) {}
    public record ShareLink(String token) {}
    public record PublicImage(String name, String albumTitle, String communityName) {}
    public record ImageComment(Long id, Long authorId, String authorName, boolean authorHasAvatar,
                               String content, OffsetDateTime createdAt, long likeCount, boolean liked) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final FileStorageService storage;
    private final CommunityMemoryService memories;
    private final GalleryThumbnailService thumbnails;

    public CommunityAlbumService(JdbcClient jdbc, CommunityService communities, FileStorageService storage,
                                 CommunityMemoryService memories, GalleryThumbnailService thumbnails) {
        this.jdbc = jdbc;
        this.communities = communities;
        this.storage = storage;
        this.memories = memories;
        this.thumbnails = thumbnails;
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

    public ImageData thumbnail(Long groupId, Long albumId, Long imageId, String email) {
        requireImageMember(groupId, albumId, imageId, email);
        return new ImageData("image/jpeg", thumbnails.readOrCreate(imageId,
            () -> read(groupId, albumId, imageId, email).data()));
    }

    @Transactional(readOnly = true)
    public List<Image> images(Long groupId, Long albumId, String email) {
        var member = communities.requireMembership(groupId, email);
        requireAlbum(groupId, albumId);
        return imageDetails(albumId, member.getUser().getId(), member.getRole() != CommunityRole.MEMBER, null);
    }

    private List<Image> imageDetails(Long albumId, Long userId, boolean manager, Long imageId) {
        return jdbc.sql("""
            SELECT f.id, f.original_name AS name, f.created_at, f.owner_id, i.taken_on, i.taken_year,
                   (SELECT COUNT(*) FROM community_image_comments c WHERE c.image_id = f.id) AS comment_count,
                   (SELECT COUNT(*) FROM community_image_likes r WHERE r.image_id = f.id) AS like_count,
                   EXISTS (SELECT 1 FROM community_image_likes r
                           WHERE r.image_id = f.id AND r.user_id = :userId) AS liked
            FROM community_album_images i JOIN stored_files f ON f.id = i.file_id
            WHERE i.album_id = :albumId
            """ + (imageId == null ? "" : " AND f.id = :imageId") + " ORDER BY f.id DESC")
            .param("albumId", albumId).param("userId", userId).param("imageId", imageId)
            .query((rs, row) -> new Image(rs.getLong("id"), rs.getString("name"), rs.getLong("comment_count"),
                rs.getLong("like_count"), rs.getBoolean("liked"),
                memories.describe(rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                    rs.getObject("taken_on", LocalDate.class), rs.getObject("taken_year", Integer.class),
                    manager || rs.getLong("owner_id") == userId))).list();
    }

    public Image upload(Long groupId, Long albumId, String email, MultipartFile file,
                        LocalDate takenOn, Integer takenYear) {
        communities.requireManager(groupId, email);
        requireAlbum(groupId, albumId);
        memories.validate(takenOn, takenYear);
        validateHeader(file);
        var saved = storage.store(email, file, FilePurpose.COMMUNITY_IMAGE, MAX_IMAGE_SIZE, IMAGE_TYPES);
        jdbc.sql("""
            INSERT INTO community_album_images (album_id, file_id, taken_on, taken_year)
            VALUES (:albumId, :fileId, :takenOn, :takenYear)
            """).param("albumId", albumId).param("fileId", saved.getId())
            .param("takenOn", takenOn).param("takenYear", takenYear).update();
        return new Image(saved.getId(), saved.getOriginalName(), 0, 0, false,
            memories.describe(saved.getCreatedAt(), takenOn, takenYear, true));
    }

    public Image updateDate(Long groupId, Long albumId, Long imageId, String email,
                            LocalDate takenOn, Integer takenYear) {
        var member = communities.requireMembership(groupId, email);
        Long ownerId = jdbc.sql("""
            SELECT f.owner_id FROM community_album_images i JOIN stored_files f ON f.id = i.file_id
            JOIN community_albums a ON a.id = i.album_id
            WHERE a.community_id = :groupId AND a.id = :albumId AND i.file_id = :imageId FOR UPDATE
            """).param("groupId", groupId).param("albumId", albumId).param("imageId", imageId)
            .query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        boolean manager = member.getRole() != CommunityRole.MEMBER;
        if (!manager && !ownerId.equals(member.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo quien subió la foto o el equipo del grupo puede cambiar su fecha");
        }
        memories.validate(takenOn, takenYear);
        jdbc.sql("UPDATE community_album_images SET taken_on = :takenOn, taken_year = :takenYear WHERE file_id = :imageId")
            .param("takenOn", takenOn).param("takenYear", takenYear).param("imageId", imageId).update();
        return imageDetails(albumId, member.getUser().getId(), manager, imageId).get(0);
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

    @Transactional(readOnly = true)
    public List<ImageLiker> imageLikers(Long groupId, Long albumId, Long imageId, String email) {
        requireImageMember(groupId, albumId, imageId, email);
        return jdbc.sql("""
            SELECT u.id AS user_id, u.name,
                   (u.avatar_file_id IS NOT NULL OR COALESCE(OCTET_LENGTH(u.avatar_data), 0) > 0) AS has_avatar
            FROM community_image_likes r
            JOIN users u ON u.id = r.user_id
            WHERE r.image_id = :imageId
            ORDER BY UPPER(u.name), u.id
            """).param("imageId", imageId).query(ImageLiker.class).list();
    }

    public ShareLink createShareLink(Long groupId, Long albumId, Long imageId, String email) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        return jdbc.sql("""
            SELECT token FROM community_image_share_links
            WHERE image_id = :imageId AND created_by_user_id = :userId AND revoked_at IS NULL
            """).param("imageId", imageId).param("userId", userId).query(String.class).optional()
            .map(ShareLink::new).orElseGet(() -> restoreShareLink(imageId, userId));
    }

    public void revokeShareLink(Long groupId, Long albumId, Long imageId, String email) {
        Long userId = requireImageMember(groupId, albumId, imageId, email);
        jdbc.sql("""
            UPDATE community_image_share_links SET revoked_at = CURRENT_TIMESTAMP
            WHERE image_id = :imageId AND created_by_user_id = :userId AND revoked_at IS NULL
            """).param("imageId", imageId).param("userId", userId).update();
    }

    @Transactional(readOnly = true)
    public PublicImage publicImage(String token) {
        return jdbc.sql("""
            SELECT f.original_name AS name, a.title AS album_title, c.name AS community_name
            FROM community_image_share_links s
            JOIN community_album_images i ON i.file_id = s.image_id
            JOIN community_albums a ON a.id = i.album_id
            JOIN communities c ON c.id = a.community_id
            JOIN stored_files f ON f.id = i.file_id
            WHERE s.token = :token AND s.revoked_at IS NULL
            """).param("token", token).query(PublicImage.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace no disponible"));
    }

    @Transactional(readOnly = true)
    public ImageData publicImageData(String token) {
        return jdbc.sql("""
            SELECT f.content_type, f.data, f.object_key
            FROM community_image_share_links s
            JOIN stored_files f ON f.id = s.image_id
            WHERE s.token = :token AND s.revoked_at IS NULL
            """).param("token", token)
            .query((rs, row) -> new ImageData(rs.getString("content_type"),
                storage.readBytes(rs.getString("object_key"), rs.getBytes("data"))))
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace no disponible"));
    }

    private ShareLink restoreShareLink(Long imageId, Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        int updated = jdbc.sql("""
            UPDATE community_image_share_links
            SET token = :token, created_at = CURRENT_TIMESTAMP, revoked_at = NULL
            WHERE image_id = :imageId AND created_by_user_id = :userId
            """).param("token", token).param("imageId", imageId).param("userId", userId).update();
        if (updated == 0) {
            jdbc.sql("""
                INSERT INTO community_image_share_links (image_id, created_by_user_id, token)
                VALUES (:imageId, :userId, :token)
                """).param("imageId", imageId).param("userId", userId).param("token", token).update();
        }
        return new ShareLink(token);
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
