package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SavedItemsService {
    public enum Kind { POST, IMAGE, EVENT }
    public enum Filter { ALL, POST, IMAGE, EVENT }
    public record Item(Kind kind, Long targetId, Long groupId, String groupName, Long albumId,
                       String title, String excerpt, OffsetDateTime savedAt) {}
    public record Page(List<Item> items, long total, int page, int size) {}
    public record State(List<Long> postIds, List<Long> imageIds, List<Long> eventIds) {}
    private static final int PAGE_SIZE = 20;
    private static final String ITEMS = """
        SELECT 'POST' AS kind, p.id AS target_id, c.id AS group_id, c.name AS group_name,
               CAST(NULL AS BIGINT) AS album_id,
               COALESCE(NULLIF(p.title, ''), 'Publicación de ' || u.name) AS title,
               p.content AS excerpt, s.saved_at
        FROM saved_items s
        JOIN community_posts p ON p.id = s.post_id
        JOIN users u ON u.id = p.author_user_id
        JOIN communities c ON c.id = p.community_id
        JOIN community_memberships m ON m.community_id = c.id AND m.user_id = s.user_id
        WHERE s.user_id = :userId
        UNION ALL
        SELECT 'IMAGE' AS kind, i.file_id AS target_id, c.id AS group_id, c.name AS group_name,
               a.id AS album_id, f.original_name AS title, a.title AS excerpt, s.saved_at
        FROM saved_items s
        JOIN community_album_images i ON i.file_id = s.image_id
        JOIN stored_files f ON f.id = i.file_id
        JOIN community_albums a ON a.id = i.album_id
        JOIN communities c ON c.id = a.community_id
        JOIN community_memberships m ON m.community_id = c.id AND m.user_id = s.user_id
        WHERE s.user_id = :userId
        UNION ALL
        SELECT 'EVENT' AS kind, e.id AS target_id, c.id AS group_id, c.name AS group_name,
               CAST(NULL AS BIGINT) AS album_id, e.title, e.description AS excerpt, s.saved_at
        FROM saved_event_items s
        JOIN community_events e ON e.id = s.event_id
        JOIN communities c ON c.id = e.community_id
        JOIN community_memberships m ON m.community_id = c.id AND m.user_id = s.user_id
        WHERE s.user_id = :userId
        """;
    private static final String FILTERED = " FROM (" + ITEMS + ") saved WHERE (:kind = 'ALL' OR kind = :kind)";
    private final JdbcClient jdbc;
    private final CommunityService communities;

    public SavedItemsService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public Page findAll(String email, Filter filter, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página no es válida");
        Long userId = userId(email);
        var params = Map.of("userId", userId, "kind", filter.name(), "offset", (long) page * PAGE_SIZE);
        long total = jdbc.sql("SELECT COUNT(*)" + FILTERED).params(params).query(Long.class).single();
        var items = jdbc.sql("SELECT *" + FILTERED
                + " ORDER BY saved_at DESC, target_id DESC, kind LIMIT " + PAGE_SIZE + " OFFSET :offset")
            .params(params).query(Item.class).list();
        return new Page(items, total, page, PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public State state(Long groupId, String email) {
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        List<Long> posts = jdbc.sql("""
            SELECT s.post_id FROM saved_items s JOIN community_posts p ON p.id = s.post_id
            WHERE s.user_id = :userId AND p.community_id = :groupId
            """).param("userId", userId).param("groupId", groupId).query(Long.class).list();
        List<Long> images = jdbc.sql("""
            SELECT s.image_id FROM saved_items s JOIN community_album_images i ON i.file_id = s.image_id
            JOIN community_albums a ON a.id = i.album_id
            WHERE s.user_id = :userId AND a.community_id = :groupId
            """).param("userId", userId).param("groupId", groupId).query(Long.class).list();
        List<Long> events = jdbc.sql("""
            SELECT s.event_id FROM saved_event_items s JOIN community_events e ON e.id = s.event_id
            WHERE s.user_id = :userId AND e.community_id = :groupId
            """).param("userId", userId).param("groupId", groupId).query(Long.class).list();
        return new State(posts, images, events);
    }

    public void save(String email, Long groupId, Kind kind, Long targetId) {
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        lockUser(userId);
        String targetQuery = switch (kind) {
            case POST -> """
            SELECT id FROM community_posts WHERE id = :targetId AND community_id = :groupId FOR UPDATE
            """;
            case IMAGE -> """
            SELECT i.file_id FROM community_album_images i JOIN community_albums a ON a.id = i.album_id
            WHERE i.file_id = :targetId AND a.community_id = :groupId FOR UPDATE
            """;
            case EVENT -> """
            SELECT id FROM community_events WHERE id = :targetId AND community_id = :groupId FOR UPDATE
            """;
        };
        jdbc.sql(targetQuery).param("targetId", targetId).param("groupId", groupId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El elemento no está disponible"));
        String column = column(kind);
        String table = kind == Kind.EVENT ? "saved_event_items" : "saved_items";
        boolean exists = jdbc.sql("SELECT user_id FROM " + table + " WHERE user_id = :userId AND " + column + " = :targetId")
            .param("userId", userId).param("targetId", targetId).query(Long.class).optional().isPresent();
        if (!exists) {
            jdbc.sql("INSERT INTO " + table + " (user_id, " + column + ") VALUES (:userId, :targetId)")
                .param("userId", userId).param("targetId", targetId).update();
        }
    }

    public void remove(String email, Kind kind, Long targetId) {
        Long userId = userId(email);
        lockUser(userId);
        String table = kind == Kind.EVENT ? "saved_event_items" : "saved_items";
        jdbc.sql("DELETE FROM " + table + " WHERE user_id = :userId AND " + column(kind) + " = :targetId")
            .param("userId", userId).param("targetId", targetId).update();
    }

    private Long userId(String email) {
        return jdbc.sql("SELECT id FROM users WHERE LOWER(email) = LOWER(:email)")
            .param("email", email).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private String column(Kind kind) {
        return switch (kind) {
            case POST -> "post_id";
            case IMAGE -> "image_id";
            case EVENT -> "event_id";
        };
    }

    private void lockUser(Long userId) {
        jdbc.sql("SELECT id FROM users WHERE id = :userId FOR UPDATE").param("userId", userId).query(Long.class).single();
    }
}
