package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityRankingService {
    static final int POST_POINTS = 5;
    static final int COMMENT_POINTS = 2;
    private static final int CONVERSATION_THRESHOLD = 5;
    private static final int CONTRIBUTOR_THRESHOLD = 25;

    public enum Badge { FIRST_POST, CONVERSATION, CONTRIBUTOR }

    public record Entry(Long userId, String name, Long position, long posts, long comments,
                        long points, List<Badge> badges, boolean viewer) {}

    public record Ranking(String season, String firstSeason, String currentSeason,
                          List<Entry> leaders, Entry viewer) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityRankingService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public Ranking find(Long groupId, String email, String requestedSeason) {
        var member = communities.requireMembership(groupId, email);
        Long viewerId = member.getUser().getId();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth first = YearMonth.from(member.getCommunity().getCreatedAt().atOffset(ZoneOffset.UTC));
        YearMonth season = parseSeason(requestedSeason, first, current);
        OffsetDateTime from = season.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime until = from.plusMonths(1);

        List<Entry> entries = jdbc.sql("""
            WITH activity AS (
                SELECT author_user_id AS user_id, COUNT(*) AS posts, 0 AS comments
                FROM community_posts
                WHERE community_id = :groupId AND created_at >= :from AND created_at < :until
                GROUP BY author_user_id
                UNION ALL
                SELECT c.author_user_id, 0, COUNT(*)
                FROM community_post_comments c JOIN community_posts p ON p.id = c.post_id
                WHERE p.community_id = :groupId AND c.created_at >= :from AND c.created_at < :until
                GROUP BY c.author_user_id
            ), totals AS (
                SELECT u.id AS user_id, u.name,
                    COALESCE(SUM(a.posts), 0) AS posts, COALESCE(SUM(a.comments), 0) AS comments,
                    COALESCE(SUM(a.posts), 0) * :postPoints + COALESCE(SUM(a.comments), 0) * :commentPoints AS points
                FROM community_memberships m JOIN users u ON u.id = m.user_id
                LEFT JOIN activity a ON a.user_id = u.id
                WHERE m.community_id = :groupId AND u.active = TRUE
                GROUP BY u.id, u.name
            ), ranked AS (
                SELECT *, RANK() OVER (ORDER BY points DESC) AS ranking_position,
                    ROW_NUMBER() OVER (ORDER BY points DESC, user_id) AS list_order
                FROM totals
            )
            SELECT user_id, name, posts, comments, points, ranking_position, list_order
            FROM ranked WHERE (points > 0 AND list_order <= 20) OR user_id = :viewerId
            ORDER BY list_order
            """).param("groupId", groupId).param("viewerId", viewerId)
            .param("from", from).param("until", until)
            .param("postPoints", POST_POINTS).param("commentPoints", COMMENT_POINTS)
            .query((rs, row) -> {
                long posts = rs.getLong("posts");
                long comments = rs.getLong("comments");
                long points = rs.getLong("points");
                Long userId = rs.getLong("user_id");
                Long position = points > 0 ? rs.getLong("ranking_position") : null;
                return new Entry(userId, rs.getString("name"), position, posts, comments,
                    points, badges(posts, comments, points), viewerId.equals(userId));
            }).list();

        Entry viewer = entries.stream().filter(Entry::viewer).findFirst().orElse(null);
        List<Entry> leaders = entries.stream().filter(entry -> entry.position() != null).limit(20).toList();
        return new Ranking(season.toString(), first.toString(), current.toString(), leaders, viewer);
    }

    private YearMonth parseSeason(String requested, YearMonth first, YearMonth current) {
        if (requested == null) return current;
        try {
            YearMonth season = YearMonth.parse(requested);
            if (!season.isBefore(first) && !season.isAfter(current)) return season;
        } catch (DateTimeParseException ignored) {
            // Use the same error for malformed and unavailable seasons.
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una temporada mensual válida");
    }

    private List<Badge> badges(long posts, long comments, long points) {
        List<Badge> badges = new ArrayList<>();
        if (posts >= 1) badges.add(Badge.FIRST_POST);
        if (comments >= CONVERSATION_THRESHOLD) badges.add(Badge.CONVERSATION);
        if (points >= CONTRIBUTOR_THRESHOLD) badges.add(Badge.CONTRIBUTOR);
        return badges;
    }
}