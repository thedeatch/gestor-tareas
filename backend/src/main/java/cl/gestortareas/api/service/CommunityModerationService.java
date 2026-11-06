package cl.gestortareas.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.NotificationType;

@Service
public class CommunityModerationService {
    private final JdbcClient jdbc;
    private final NotificationService notifications;

    public CommunityModerationService(JdbcClient jdbc, NotificationService notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    public void recordRemoval(CommunityMembership moderator, AppUser author, Long postId,
                              Long commentId, String reason) {
        String normalized = reason == null ? "" : reason.strip();
        if (normalized.isBlank() || normalized.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Indica un motivo de moderación de entre 1 y 500 caracteres");
        }

        Long communityId = moderator.getCommunity().getId();
        jdbc.sql("""
            INSERT INTO community_moderation_log
                (community_id, post_id, comment_id, actor_user_id, author_user_id, reason)
            VALUES (:communityId, :postId, :commentId, :actorId, :authorId, :reason)
            """)
            .param("communityId", communityId)
            .param("postId", postId)
            .param("commentId", commentId, java.sql.Types.BIGINT)
            .param("actorId", moderator.getUser().getId())
            .param("authorId", author.getId())
            .param("reason", normalized)
            .update();

        String title = commentId == null
            ? "Moderación retiró tu publicación"
            : "Moderación retiró tu comentario";
        notifications.create(author, moderator.getUser(), NotificationType.MODERATION,
            title, normalized, "/grupos/" + communityId);
    }
}