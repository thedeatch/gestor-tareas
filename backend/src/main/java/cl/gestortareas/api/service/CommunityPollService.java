package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.CommunityRole;

@Service
@Transactional
public class CommunityPollService {
    public record PollOption(Long id, String label, long votes) {}
    public record Poll(Long id, String question, Long authorId, String authorName,
                       OffsetDateTime createdAt, OffsetDateTime closesAt, boolean closed,
                       boolean canClose, Long selectedOptionId, long totalVotes, List<PollOption> options) {}
    private record PollRow(Long id, String question, Long authorId, String authorName,
                           OffsetDateTime createdAt, OffsetDateTime closesAt, OffsetDateTime closedAt,
                           Long selectedOptionId) {}
    private record OptionRow(Long id, Long pollId, String label, int position, long votes) {}
    private record PollState(Long authorId, OffsetDateTime closesAt, OffsetDateTime closedAt) {}

    private static final String SELECT_POLLS = """
        SELECT p.id, p.question, p.author_user_id AS author_id, u.name AS author_name,
               p.created_at, p.closes_at, p.closed_at, v.option_id AS selected_option_id
        FROM community_polls p JOIN users u ON u.id = p.author_user_id
        LEFT JOIN community_poll_votes v ON v.poll_id = p.id AND v.user_id = :userId
        WHERE p.community_id = :groupId
        """;
    private static final String SELECT_OPTIONS = """
        SELECT o.id, o.poll_id, o.label, o.position, COUNT(v.user_id) AS votes
        FROM community_poll_options o JOIN community_polls p ON p.id = o.poll_id
        LEFT JOIN community_poll_votes v ON v.poll_id = o.poll_id AND v.option_id = o.id
        WHERE p.community_id = :groupId
        """;

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityPollService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Poll> findAll(Long groupId, String email) {
        var member = communities.requireMembership(groupId, email);
        return load(groupId, member.getUser().getId(), member.getRole(), null);
    }

    public Poll create(Long groupId, String email, String question, List<String> options,
                       OffsetDateTime closesAt) {
        var member = communities.requireMembership(groupId, email);
        List<String> labels = options.stream().map(String::strip).toList();
        long distinct = labels.stream().map(label -> label.toLowerCase(Locale.ROOT)).distinct().count();
        if (distinct != labels.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las opciones deben ser diferentes");
        }
        if (closesAt != null && !closesAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de cierre debe ser futura");
        }
        var key = new GeneratedKeyHolder();
        jdbc.sql("""
            INSERT INTO community_polls (community_id, author_user_id, question, closes_at)
            VALUES (:groupId, :userId, :question, :closesAt)
            """).param("groupId", groupId).param("userId", member.getUser().getId())
            .param("question", question.strip()).param("closesAt", closesAt).update(key, "id");
        Long pollId = key.getKeyAs(Long.class);
        for (int position = 0; position < labels.size(); position++) {
            jdbc.sql("INSERT INTO community_poll_options (poll_id, label, position) VALUES (:pollId, :label, :position)")
                .param("pollId", pollId).param("label", labels.get(position)).param("position", position).update();
        }
        return load(groupId, member.getUser().getId(), member.getRole(), pollId).get(0);
    }

    public Poll vote(Long groupId, Long pollId, String email, Long optionId) {
        var member = communities.requireMembership(groupId, email);
        PollState poll = lock(groupId, pollId);
        if (isClosed(poll.closedAt(), poll.closesAt(), OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La encuesta está cerrada");
        }
        jdbc.sql("SELECT id FROM community_poll_options WHERE id = :optionId AND poll_id = :pollId")
            .param("optionId", optionId).param("pollId", pollId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La opción no pertenece a esta encuesta"));
        Long userId = member.getUser().getId();
        jdbc.sql("DELETE FROM community_poll_votes WHERE poll_id = :pollId AND user_id = :userId")
            .param("pollId", pollId).param("userId", userId).update();
        jdbc.sql("INSERT INTO community_poll_votes (poll_id, user_id, option_id) VALUES (:pollId, :userId, :optionId)")
            .param("pollId", pollId).param("userId", userId).param("optionId", optionId).update();
        return load(groupId, userId, member.getRole(), pollId).get(0);
    }

    public Poll close(Long groupId, Long pollId, String email) {
        var member = communities.requireMembership(groupId, email);
        PollState poll = lock(groupId, pollId);
        if (!poll.authorId().equals(member.getUser().getId())) {
            communities.requireManager(groupId, email);
        }
        jdbc.sql("UPDATE community_polls SET closed_at = COALESCE(closed_at, :now) WHERE id = :pollId")
            .param("now", OffsetDateTime.now()).param("pollId", pollId).update();
        return load(groupId, member.getUser().getId(), member.getRole(), pollId).get(0);
    }

    private PollState lock(Long groupId, Long pollId) {
        return jdbc.sql("""
            SELECT author_user_id AS author_id, closes_at, closed_at FROM community_polls
            WHERE community_id = :groupId AND id = :pollId FOR UPDATE
            """).param("groupId", groupId).param("pollId", pollId).query(PollState.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
    }

    private List<Poll> load(Long groupId, Long userId, CommunityRole role, Long pollId) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("userId", userId);
        String condition = "";
        if (pollId != null) {
            condition = " AND p.id = :pollId";
            params.put("pollId", pollId);
        }
        var rows = jdbc.sql(SELECT_POLLS + condition + " ORDER BY p.id DESC")
            .params(params).query(PollRow.class).list();
        var options = jdbc.sql(SELECT_OPTIONS + condition + """
             GROUP BY o.id, o.poll_id, o.label, o.position ORDER BY o.position, o.id
            """).params(params).query(OptionRow.class).list().stream()
            .collect(Collectors.groupingBy(OptionRow::pollId));
        OffsetDateTime now = OffsetDateTime.now();
        List<Poll> polls = new ArrayList<>();
        for (var row : rows) {
            List<PollOption> choices = options.getOrDefault(row.id(), List.of()).stream()
                .map(option -> new PollOption(option.id(), option.label(), option.votes())).toList();
            polls.add(new Poll(row.id(), row.question(), row.authorId(), row.authorName(),
                row.createdAt(), row.closesAt(), isClosed(row.closedAt(), row.closesAt(), now),
                row.authorId().equals(userId) || role != CommunityRole.MEMBER, row.selectedOptionId(),
                choices.stream().mapToLong(PollOption::votes).sum(), choices));
        }
        return polls;
    }

    private boolean isClosed(OffsetDateTime closedAt, OffsetDateTime closesAt, OffsetDateTime now) {
        return closedAt != null || (closesAt != null && !closesAt.isAfter(now));
    }
}
