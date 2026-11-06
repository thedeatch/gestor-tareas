package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CommunityEventService {
    public record Event(Long id, String title, String description, OffsetDateTime startsAt,
                        long participantCount, boolean registered) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityEventService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Event> findAll(Long groupId, String email) {
        var member = communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT e.id, e.title, e.description, e.starts_at, COUNT(r.user_id) AS participant_count,
                COUNT(CASE WHEN r.user_id = :userId THEN 1 END) > 0 AS registered
            FROM community_events e
            LEFT JOIN community_event_registrations r ON r.event_id = e.id
            WHERE e.community_id = :groupId
            GROUP BY e.id, e.title, e.description, e.starts_at
            ORDER BY CASE WHEN e.starts_at >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END, e.starts_at, e.id
            """).param("groupId", groupId).param("userId", member.getUser().getId())
            .query(Event.class).list();
    }

    public void create(Long groupId, String email, String title, String description, OffsetDateTime startsAt) {
        var manager = communities.requireManager(groupId, email);
        requireFuture(startsAt, "La fecha del evento debe ser futura");
        jdbc.sql("""
            INSERT INTO community_events (community_id, created_by, title, description, starts_at)
            VALUES (:groupId, :creator, :title, :description, :startsAt)
            """).param("groupId", groupId).param("creator", manager.getUser().getId())
            .param("title", title.strip()).param("description", description.strip())
            .param("startsAt", startsAt).update();
    }

    public void setRegistration(Long groupId, Long eventId, String email, boolean registered) {
        Long userId = communities.requireMembership(groupId, email).getUser().getId();
        var startsAt = jdbc.sql("""
            SELECT starts_at FROM community_events
            WHERE id = :id AND community_id = :groupId FOR UPDATE
            """).param("id", eventId).param("groupId", groupId).query(OffsetDateTime.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        requireFuture(startsAt, "El evento ya comenzó");

        if (registered) {
            jdbc.sql("""
                INSERT INTO community_event_registrations (event_id, user_id)
                SELECT :id, :userId WHERE NOT EXISTS (
                    SELECT 1 FROM community_event_registrations WHERE event_id = :id AND user_id = :userId
                )
                """).param("id", eventId).param("userId", userId).update();
        } else {
            jdbc.sql("""
                DELETE FROM community_event_registrations WHERE event_id = :id AND user_id = :userId
                """).param("id", eventId).param("userId", userId).update();
        }
    }

    private void requireFuture(OffsetDateTime startsAt, String message) {
        if (!startsAt.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}