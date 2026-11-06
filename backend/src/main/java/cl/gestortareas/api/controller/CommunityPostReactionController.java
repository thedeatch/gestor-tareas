package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.CommunityPostReaction;
import cl.gestortareas.api.dto.CommunityPostReaction.Type;
import cl.gestortareas.api.service.CommunityPostService;

@RestController
@RequestMapping("/api/communities/{communityId}/posts/{postId}/reactions/{type}")
public class CommunityPostReactionController {
    private final CommunityPostService posts;

    public CommunityPostReactionController(CommunityPostService posts) {
        this.posts = posts;
    }

    @PutMapping
    public List<CommunityPostReaction> add(@PathVariable Long communityId, @PathVariable Long postId,
        @PathVariable Type type, Authentication auth) {
        return posts.setReaction(communityId, postId, auth.getName(), type, true);
    }

    @DeleteMapping
    public List<CommunityPostReaction> remove(@PathVariable Long communityId, @PathVariable Long postId,
        @PathVariable Type type, Authentication auth) {
        return posts.setReaction(communityId, postId, auth.getName(), type, false);
    }
}