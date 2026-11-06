package cl.gestortareas.api.service;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.CommunityMembership;

@Service
@Transactional
public class CommunityReportService {
    public enum Resolution { DISMISSED, REMOVED }

    public record Report(Long id, Long postId, Long commentId, String reason,
                         OffsetDateTime createdAt, String content) {}

    private record Target(Long postId, Long commentId) {}

    private final JdbcClient jdbc;
    private final CommunityPostService posts;
    private final CommunityPostCommentService comments;

    public CommunityReportService(JdbcClient jdbc, CommunityPostService posts,
                                  CommunityPostCommentService comments) {
        this.jdbc = jdbc;
        this.posts = posts;
        this.comments = comments;
    }

    public void create(Long groupId, Long postId, Long commentId, String reason, String email) {
        var member = posts.requireMembership(groupId, email);
        // The post lock serializes submissions for this target and coordinates with deletion.
        var post = posts.lockPost(groupId, postId);
        var author = commentId == null ? post.getAuthor() : comments.requireComment(postId, commentId).getAuthor();
        Long reporter = member.getUser().getId();
        if (reporter.equals(author.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes reportar tu propio contenido");
        }

        int inserted = jdbc.sql("""
            INSERT INTO community_reports (community_id, post_id, comment_id, reporter_id, reason)
            SELECT :groupId, :postId, :commentId, :reporter, :reason
            WHERE NOT EXISTS (
                SELECT 1 FROM community_reports
                WHERE post_id = :postId AND comment_id IS NOT DISTINCT FROM :commentId
                    AND reporter_id = :reporter AND resolution IS NULL
            )
            """).param("groupId", groupId).param("postId", postId)
            .param("commentId", commentId, Types.BIGINT).param("reporter", reporter)
            .param("reason", reason.strip()).update();
        if (inserted == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes un reporte pendiente para este contenido");
        }
    }

    @Transactional(readOnly = true)
    public List<Report> pending(Long groupId, String email) {
        requireModerator(groupId, email);
        return jdbc.sql("""
            SELECT r.id, r.post_id, r.comment_id, r.reason, r.created_at,
                CASE WHEN r.comment_id IS NULL THEN p.content ELSE c.content END AS content
            FROM community_reports r
            LEFT JOIN community_posts p ON p.id = r.post_id
            LEFT JOIN community_post_comments c ON c.id = r.comment_id
            WHERE r.community_id = :groupId AND r.resolution IS NULL
            ORDER BY r.created_at, r.id
            """).param("groupId", groupId).query(Report.class).list();
    }

    public void resolve(Long groupId, Long id, Resolution resolution, String reason, String email) {
        var moderator = requireModerator(groupId, email);
        var target = jdbc.sql("""
            SELECT post_id, comment_id FROM community_reports
            WHERE id = :id AND community_id = :groupId AND resolution IS NULL FOR UPDATE
            """).param("id", id).param("groupId", groupId).query(Target.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte pendiente no encontrado"));

        String normalized = reason.strip();
        if (resolution == Resolution.REMOVED) {
            if (target.commentId() == null) {
                posts.delete(groupId, target.postId(), email, normalized);
            } else {
                comments.delete(groupId, target.postId(), target.commentId(), email, normalized);
            }
        }
        jdbc.sql("""
            UPDATE community_reports
            SET resolution = :resolution, resolved_by = :actor,
                resolved_at = CURRENT_TIMESTAMP, resolution_reason = :reason
            WHERE id = :id
            """).param("resolution", resolution.name()).param("actor", moderator.getUser().getId())
            .param("reason", normalized).param("id", id).update();
    }

    private CommunityMembership requireModerator(Long groupId, String email) {
        var member = posts.requireMembership(groupId, email);
        if (!CommunityPostService.MODERATION_ROLES.contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para revisar reportes");
        }
        return member;
    }
}