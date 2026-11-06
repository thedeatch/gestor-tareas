package cl.gestortareas.api.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import cl.gestortareas.api.dto.CommunityPostReaction;
import cl.gestortareas.api.dto.CommunityPostReaction.Type;

@Repository
public class CommunityPostReactionRepository {
    private final JdbcClient jdbc;

    public CommunityPostReactionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, List<CommunityPostReaction>> summarize(List<Long> postIds, Long viewerId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return jdbc.sql("""
            SELECT post_id, type, COUNT(*) AS total,
                MAX(CASE WHEN user_id = :viewerId THEN 1 ELSE 0 END) AS selected
            FROM community_post_reactions
            WHERE post_id IN (:postIds)
            GROUP BY post_id, type
            """)
            .param("viewerId", viewerId)
            .param("postIds", postIds)
            .query(rs -> {
                Map<Long, List<CommunityPostReaction>> result = new HashMap<>();
                while (rs.next()) {
                    var reaction = new CommunityPostReaction(Type.valueOf(rs.getString("type")),
                        rs.getLong("total"), rs.getInt("selected") == 1);
                    result.computeIfAbsent(rs.getLong("post_id"), id -> new ArrayList<>()).add(reaction);
                }
                return result;
            });
    }

    public boolean add(Long postId, Long userId, Type type) {
        return jdbc.sql("""
            INSERT INTO community_post_reactions (post_id, user_id, type)
            SELECT :postId, :userId, :type
            WHERE NOT EXISTS (
                SELECT 1 FROM community_post_reactions
                WHERE post_id = :postId AND user_id = :userId AND type = :type
            )
            """)
            .param("postId", postId).param("userId", userId).param("type", type.name())
            .update() == 1;
    }

    public void remove(Long postId, Long userId, Type type) {
        jdbc.sql("""
            DELETE FROM community_post_reactions
            WHERE post_id = :postId AND user_id = :userId AND type = :type
            """)
            .param("postId", postId).param("userId", userId).param("type", type.name())
            .update();
    }
}