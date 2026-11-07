package cl.gestortareas.api.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityPostComment;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.CommunityPostCommentRepository;

@Service
@Transactional
public class CommunityPostCommentService {
    public record Comment(Long id, Long authorId, String authorName, boolean authorHasAvatar,
                          Long parentId, String content, Instant createdAt, Instant updatedAt,
                          boolean editable, boolean deletable) {}

    private final CommunityPostService posts;
    private final CommunityPostCommentRepository comments;
    private final NotificationService notifications;
    private final CommunityModerationService moderation;
    private final CommunityAchievementService achievements;

    public CommunityPostCommentService(CommunityPostService posts,
        CommunityPostCommentRepository comments, NotificationService notifications,
        CommunityModerationService moderation,
        CommunityAchievementService achievements) {
        this.posts = posts;
        this.comments = comments;
        this.notifications = notifications;
        this.moderation = moderation;
        this.achievements = achievements;
    }

    @Transactional(readOnly = true)
    public List<Comment> findAll(Long groupId, Long postId, String email) {
        var member = posts.requireMembership(groupId, email);
        posts.requirePost(groupId, postId);
        return comments.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).stream()
            .map(comment -> response(comment, member)).toList();
    }

    public Comment create(Long groupId, Long postId, String email, String content, Long parentId) {
        var member = posts.requireMembership(groupId, email);
        var post = posts.requirePost(groupId, postId);
        var actor = member.getUser();
        var recipients = new LinkedHashSet<AppUser>();
        recipients.add(post.getAuthor());
        if (parentId != null) {
            var parentAuthor = requireComment(postId, parentId).getAuthor();
            if (!parentAuthor.getId().equals(post.getAuthor().getId())) {
                recipients.add(parentAuthor);
            }
        }

        var comment = comments.save(new CommunityPostComment(post, actor, parentId, content.strip()));
        achievements.recordComment(member);
        for (var recipient : recipients) {
            if (!recipient.getId().equals(actor.getId())) {
                boolean postAuthor = recipient.getId().equals(post.getAuthor().getId());
                notifications.create(recipient, actor, NotificationType.COMMENT,
                    postAuthor ? "Nuevo comentario en tu publicación" : "Respuesta a tu comentario",
                    actor.getName() + " escribió un comentario en " + member.getCommunity().getName(),
                    "/grupos/" + groupId + "?comentarios=" + postId);
            }
        }
        return response(comment, member);
    }

    public Comment edit(Long groupId, Long postId, Long id, String email, String content) {
        var member = posts.requireMembership(groupId, email);
        posts.requirePost(groupId, postId);
        var comment = requireComment(postId, id);
        if (!isAuthor(comment, member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede editar el comentario");
        }
        comment.edit(content.strip());
        return response(comment, member);
    }

    public void delete(Long groupId, Long postId, Long id, String email, String reason) {
        var member = posts.requireMembership(groupId, email);
        posts.lockPost(groupId, postId);
        var comment = requireComment(postId, id);
        boolean author = isAuthor(comment, member);
        if (!author && !CommunityPostService.MODERATION_ROLES.contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para eliminar el comentario");
        }
        if (!author) {
            moderation.recordRemoval(member, comment.getAuthor(), postId, id, reason);
        }
        comments.delete(comment);
    }

    CommunityPostComment requireComment(Long postId, Long id) {
        return comments.findByPostIdAndId(postId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));
    }

    private boolean isAuthor(CommunityPostComment comment, CommunityMembership member) {
        return comment.getAuthor().getId().equals(member.getUser().getId());
    }

    private Comment response(CommunityPostComment comment, CommunityMembership member) {
        var author = comment.getAuthor();
        boolean editable = isAuthor(comment, member);
        return new Comment(comment.getId(), author.getId(), author.getName(),
            author.isActive() && author.isPublicProfile() && author.hasAvatar(),
            comment.getParentId(), comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt(),
            editable, editable || CommunityPostService.MODERATION_ROLES.contains(member.getRole()));
    }
}