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

    public record Metrics(int days, OffsetDateTime from, OffsetDateTime until, Counts counts) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityMetricsService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
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