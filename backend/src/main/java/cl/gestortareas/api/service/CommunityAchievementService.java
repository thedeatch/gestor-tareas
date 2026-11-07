package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.gestortareas.api.model.CommunityMembership;

@Service
public class CommunityAchievementService {
    private static final int COMMENT_TARGET = 10;
    private static final int POINTS_TARGET = 50;

    public record Achievement(String code, String title, String description, long current,
                              long target, OffsetDateTime earnedAt) {}
    private record Progress(long posts, long comments) {
        long points() {
            return posts * CommunityRankingService.POST_POINTS + comments * CommunityRankingService.COMMENT_POINTS;
        }
    }
    private record Award(String code, OffsetDateTime earnedAt) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityAchievementService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Achievement> mine(Long groupId, String email) {
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        var progress = progress(groupId, userId);
        var awards = jdbc.sql("""
            SELECT code, earned_at FROM community_achievements
            WHERE community_id = :groupId AND user_id = :userId
            """).param("groupId", groupId).param("userId", userId).query(Award.class).list()
            .stream().collect(Collectors.toMap(Award::code, Award::earnedAt));
        return achievements(progress, awards);
    }

    public void recordPost(CommunityMembership member) {
        record(member, 1, 0);
    }

    public void recordComment(CommunityMembership member) {
        record(member, 0, 1);
    }

    private void record(CommunityMembership member, int posts, int comments) {
        Long groupId = member.getCommunity().getId();
        Long userId = member.getUser().getId();
        // Serialize counters for one member, including the first contribution.
        jdbc.sql("SELECT id FROM community_memberships WHERE id = :id FOR UPDATE")
            .param("id", member.getId()).query(Long.class).single();
        int updated = jdbc.sql("""
            UPDATE community_achievement_progress SET posts = posts + :posts, comments = comments + :comments
            WHERE community_id = :groupId AND user_id = :userId
            """).param("posts", posts).param("comments", comments)
            .param("groupId", groupId).param("userId", userId).update();
        if (updated == 0) {
            jdbc.sql("""
                INSERT INTO community_achievement_progress (community_id, user_id, posts, comments)
                VALUES (:groupId, :userId, :posts, :comments)
                """).param("groupId", groupId).param("userId", userId)
                .param("posts", posts).param("comments", comments).update();
        }
        var current = progress(groupId, userId);
        var previous = new Progress(current.posts() - posts, current.comments() - comments);
        if (posts == 1 && current.posts() == 1) award(groupId, userId, "FIRST_POST");
        if (comments == 1 && current.comments() == COMMENT_TARGET) award(groupId, userId, "TEN_COMMENTS");
        if (previous.points() < POINTS_TARGET && current.points() >= POINTS_TARGET) {
            award(groupId, userId, "FIFTY_POINTS");
        }
    }

    private Progress progress(Long groupId, Long userId) {
        return jdbc.sql("""
            SELECT posts, comments FROM community_achievement_progress
            WHERE community_id = :groupId AND user_id = :userId
            """).param("groupId", groupId).param("userId", userId).query(Progress.class).optional()
            .orElse(new Progress(0, 0));
    }

    private List<Achievement> achievements(Progress progress, Map<String, OffsetDateTime> awards) {
        return List.of(
            achievement("FIRST_POST", "Primera publicación", "Publica una vez en este grupo.",
                progress.posts(), 1, awards),
            achievement("TEN_COMMENTS", "Voz de la comunidad", "Escribe diez comentarios en este grupo.",
                progress.comments(), COMMENT_TARGET, awards),
            achievement("FIFTY_POINTS", "Trayectoria comunitaria", "Acumula 50 puntos en este grupo.",
                progress.points(), POINTS_TARGET, awards));
    }

    private Achievement achievement(String code, String title, String description, long value, long target,
                                    Map<String, OffsetDateTime> awards) {
        return new Achievement(code, title, description, Math.min(value, target), target, awards.get(code));
    }

    private void award(Long groupId, Long userId, String code) {
        jdbc.sql("""
            INSERT INTO community_achievements (community_id, user_id, code)
            SELECT :groupId, :userId, :code WHERE NOT EXISTS (
                SELECT 1 FROM community_achievements WHERE community_id = :groupId AND user_id = :userId AND code = :code
            )
            """).param("groupId", groupId).param("userId", userId).param("code", code).update();
    }
}