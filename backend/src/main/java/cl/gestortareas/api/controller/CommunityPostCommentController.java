package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import cl.gestortareas.api.service.CommunityPostCommentService;
import cl.gestortareas.api.service.CommunityPostCommentService.Comment;
import cl.gestortareas.api.dto.CommunityModerationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{communityId}/posts/{postId}/comments")
public class CommunityPostCommentController {
    public record CreateRequest(@NotBlank @Size(max = 2000) String content, Long parentId) {}
    public record EditRequest(@NotBlank @Size(max = 2000) String content) {}

    private final CommunityPostCommentService comments;

    public CommunityPostCommentController(CommunityPostCommentService comments) {
        this.comments = comments;
    }

    @GetMapping
    public List<Comment> findAll(@PathVariable Long communityId, @PathVariable Long postId,
                                 Authentication auth) {
        return comments.findAll(communityId, postId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Comment create(@PathVariable Long communityId, @PathVariable Long postId,
                          @Valid @RequestBody CreateRequest request, Authentication auth) {
        return comments.create(communityId, postId, auth.getName(), request.content(), request.parentId());
    }

    @PatchMapping("/{id}")
    public Comment edit(@PathVariable Long communityId, @PathVariable Long postId, @PathVariable Long id,
                        @Valid @RequestBody EditRequest request, Authentication auth) {
        return comments.edit(communityId, postId, id, auth.getName(), request.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long communityId, @PathVariable Long postId, @PathVariable Long id,
                       @RequestBody(required = false) CommunityModerationRequest request,
                       Authentication auth) {
        comments.delete(communityId, postId, id, auth.getName(),
            request == null ? null : request.reason());
    }
}