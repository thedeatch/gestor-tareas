package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class CommunityEventReminderService {
    public record Candidate(Long eventId, Long userId) {}
    private record Event(Long communityId, String title, OffsetDateTime startsAt) {}

    private final JdbcClient jdbc;
    private final AppUserRepository users;
    private final NotificationService notifications;

    public CommunityEventReminderService(JdbcClient jdbc, AppUserRepository users,
                                         NotificationService notifications) {
        this.jdbc = jdbc;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<Candidate> due(OffsetDateTime now) {
        return jdbc.sql("""
            SELECT e.id AS event_id, r.user_id
            FROM community_events e
            JOIN community_event_registrations r ON r.event_id = e.id
            JOIN community_memberships m ON m.community_id = e.community_id AND m.user_id = r.user_id
            JOIN users u ON u.id = r.user_id AND u.active = TRUE
            WHERE e.starts_at > :now AND e.starts_at <= :until
                AND NOT EXISTS (
                    SELECT 1 FROM community_event_reminders d WHERE d.event_id = e.id AND d.user_id = r.user_id
                )
                AND NOT EXISTS (
                    SELECT 1 FROM disabled_notification_types p WHERE p.user_id = r.user_id AND p.type = 'EVENT'
                )
            ORDER BY e.starts_at, e.id, r.user_id LIMIT 100
            """).param("now", now).param("until", now.plusHours(1)).query(Candidate.class).list();
    }

    @Transactional
    public void deliver(Candidate candidate, OffsetDateTime now) {
        // Registration changes also lock the event before mutating its participants.
        var event = jdbc.sql("""
            SELECT community_id, title, starts_at FROM community_events WHERE id = :eventId FOR UPDATE
            """).param("eventId", candidate.eventId()).query(Event.class).optional().orElse(null);
        if (event == null || !event.startsAt().isAfter(now) || event.startsAt().isAfter(now.plusHours(1))) {
            return;
        }

        var recipient = users.findById(candidate.userId()).filter(user -> user.isActive()).orElse(null);
        if (recipient == null) return;

        int inserted = jdbc.sql("""
            INSERT INTO community_event_reminders (event_id, user_id)
            SELECT :eventId, :userId
            WHERE EXISTS (
                SELECT 1 FROM community_event_registrations r
                JOIN community_memberships m ON m.community_id = :groupId AND m.user_id = r.user_id
                WHERE r.event_id = :eventId AND r.user_id = :userId
            ) AND NOT EXISTS (
                SELECT 1 FROM disabled_notification_types p WHERE p.user_id = :userId AND p.type = 'EVENT'
            ) AND NOT EXISTS (
                SELECT 1 FROM community_event_reminders d WHERE d.event_id = :eventId AND d.user_id = :userId
            )
            """).param("eventId", candidate.eventId()).param("userId", candidate.userId())
            .param("groupId", event.communityId()).update();

        if (inserted == 1) {
            boolean sent = notifications.create(recipient, null, NotificationType.EVENT, "Tu evento está por comenzar",
                "«" + event.title() + "» comienza dentro de la próxima hora.",
                "/grupos/" + event.communityId());
            if (!sent) {
                jdbc.sql("DELETE FROM community_event_reminders WHERE event_id = :eventId AND user_id = :userId")
                    .param("eventId", candidate.eventId()).param("userId", candidate.userId()).update();
            }
        }
    }
}