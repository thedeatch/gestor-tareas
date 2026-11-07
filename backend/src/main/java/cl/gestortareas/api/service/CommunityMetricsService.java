package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityMetricsService {
    private static final Set<Integer> PERIODS = Set.of(7, 30, 90);

    public record Counts(long members, long newMembers, long posts, long comments,
                         long events, long registrations, long participants) {}

    public record PersonalStats(long posts, long comments, long points, long reactionsReceived,
                                long eventRegistrations, long achievements) {}

    public record Metrics(int days, OffsetDateTime from, OffsetDateTime until, Counts counts) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityMetricsService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public PersonalStats mine(Long groupId, String email) {
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        return jdbc.sql("""
            SELECT COALESCE(a.posts, 0) AS posts, COALESCE(a.comments, 0) AS comments,
                (SELECT COUNT(*) FROM community_post_reactions r JOIN community_posts p ON p.id = r.post_id
                    WHERE p.community_id = :groupId AND p.author_user_id = :userId
                        AND r.user_id <> :userId) AS reactions_received,
                (SELECT COUNT(*) FROM community_event_registrations r JOIN community_events e ON e.id = r.event_id
                    WHERE e.community_id = :groupId AND r.user_id = :userId) AS event_registrations,
                (SELECT COUNT(*) FROM community_achievements
                    WHERE community_id = :groupId AND user_id = :userId) AS achievements
            FROM community_memberships m
            LEFT JOIN community_achievement_progress a ON a.community_id = m.community_id AND a.user_id = m.user_id
            WHERE m.community_id = :groupId AND m.user_id = :userId
            """).param("groupId", groupId).param("userId", userId).query((rs, row) -> {
                long posts = rs.getLong("posts");
                long comments = rs.getLong("comments");
                long points = posts * CommunityRankingService.POST_POINTS + comments * CommunityRankingService.COMMENT_POINTS;
                return new PersonalStats(posts, comments, points, rs.getLong("reactions_received"),
                    rs.getLong("event_registrations"), rs.getLong("achievements"));
            }).optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado"));
    }

    @Transactional(readOnly = true)
    public Metrics find(Long groupId, String email, int days) {
        communities.requireMembership(groupId, email);
        if (!PERIODS.contains(days)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un período de 7, 30 o 90 días");
        }

        OffsetDateTime until = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = until.toLocalDate().minusDays(days - 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Counts counts = jdbc.sql("""
            SELECT
                (SELECT COUNT(*) FROM community_memberships WHERE community_id = :groupId) AS members,
                (SELECT COUNT(*) FROM community_memberships WHERE community_id = :groupId
                    AND joined_at >= :from AND joined_at < :until) AS new_members,
                (SELECT COUNT(*) FROM community_posts WHERE community_id = :groupId
                    AND created_at >= :from AND created_at < :until) AS posts,
                (SELECT COUNT(*) FROM community_post_comments c JOIN community_posts p ON p.id = c.post_id
                    WHERE p.community_id = :groupId AND c.created_at >= :from AND c.created_at < :until) AS comments,
                COUNT(DISTINCT e.id) AS events,
                COUNT(r.user_id) AS registrations,
                COUNT(DISTINCT r.user_id) AS participants
            FROM community_events e LEFT JOIN community_event_registrations r ON r.event_id = e.id
            WHERE e.community_id = :groupId AND e.starts_at >= :from AND e.starts_at < :until
            """).param("groupId", groupId).param("from", from).param("until", until)
            .query(Counts.class).single();
        return new Metrics(days, from, until, counts);
    }
}