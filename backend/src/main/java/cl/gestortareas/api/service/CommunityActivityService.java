package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityActivityService {
    public record Activity(String type, Long resourceId, String title, OffsetDateTime createdAt) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityActivityService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Activity> recent(Long groupId, String email) {
        communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT type, resource_id, title, created_at FROM (
                SELECT 'POST' AS type, id AS resource_id,
                    COALESCE(title, SUBSTRING(content, 1, 120)) AS title, created_at
                FROM community_posts WHERE community_id = :groupId
                UNION ALL
                SELECT 'EVENT', id, title, created_at
                FROM community_events WHERE community_id = :groupId AND created_at IS NOT NULL
                UNION ALL
                SELECT 'ALBUM', id, title, created_at
                FROM community_albums WHERE community_id = :groupId AND created_at IS NOT NULL
            ) activity
            ORDER BY created_at DESC, type, resource_id DESC LIMIT 30
            """).param("groupId", groupId).query(Activity.class).list();
    }
}