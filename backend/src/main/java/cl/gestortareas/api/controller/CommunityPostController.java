package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.CommunityPostRequest;
import cl.gestortareas.api.dto.CommunityPostResponse;
import cl.gestortareas.api.service.CommunityPostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import cl.gestortareas.api.dto.CommunityModerationRequest;
import jakarta.validation.Valid;

@Tag(name = "Publicaciones", description = "Publicaciones dentro de los grupos")
@RestController
@RequestMapping("/api/communities/{communityId}/posts")
public class CommunityPostController {
    private final CommunityPostService postService;

    public CommunityPostController(CommunityPostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<CommunityPostResponse> findAll(@PathVariable Long communityId,
                                              Authentication authentication) {
        return postService.findAll(communityId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityPostResponse create(@PathVariable Long communityId,
        @Valid @RequestBody CommunityPostRequest request,
        Authentication authentication) {
        return postService.create(communityId, request, authentication.getName());
    }

    @PatchMapping("/{postId}")
    public CommunityPostResponse update(@PathVariable Long communityId,
        @PathVariable Long postId, @Valid @RequestBody CommunityPostRequest request,
        Authentication authentication) {
        return postService.update(communityId, postId, request,
            authentication.getName());
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long communityId, @PathVariable Long postId,
                       @RequestBody(required = false) CommunityModerationRequest request,
                       Authentication authentication) {
        postService.delete(communityId, postId, authentication.getName(),
            request == null ? null : request.reason());
    }
}
