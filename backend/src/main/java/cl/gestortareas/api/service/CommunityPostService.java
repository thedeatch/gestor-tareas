package cl.gestortareas.api.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.CommunityPostRequest;
import cl.gestortareas.api.dto.CommunityPostResponse;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityPost;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityPostRepository;

@Service
public class CommunityPostService {
    private static final Set<CommunityRole> MODERATION_ROLES = Set.of(
        CommunityRole.OWNER,
        CommunityRole.COMMUNITY_ADMIN,
        CommunityRole.COMMUNITY_MODERATOR
    );

    private final CommunityPostRepository postRepository;
    private final CommunityMembershipRepository membershipRepository;

    public CommunityPostService(CommunityPostRepository postRepository,
                                CommunityMembershipRepository membershipRepository) {
        this.postRepository = postRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> findAll(Long communityId, String email) {
        CommunityMembership membership = requireMembership(communityId, email);
        return postRepository.findAllByCommunityIdOrderByCreatedAtDesc(communityId)
            .stream().map(post -> toResponse(post, membership)).toList();
    }

    @Transactional
    public CommunityPostResponse create(Long communityId,
                                        CommunityPostRequest request,
                                        String email) {
        CommunityMembership membership = requireMembership(communityId, email);
        boolean firstPost = !postRepository.existsByCommunityId(communityId);
        String title = firstPost ? requireTitle(request.title()) : null;
        CommunityPost post = postRepository.save(new CommunityPost(
            membership.getCommunity(), membership.getUser(),
            title,
            request.content().trim()));
        return toResponse(post, membership);
    }

    @Transactional
    public CommunityPostResponse update(Long communityId, Long postId,
                                        CommunityPostRequest request,
                                        String email) {
        CommunityMembership membership = requireMembership(communityId, email);
        CommunityPost post = requirePost(communityId, postId);
        if (!post.getAuthor().getId().equals(membership.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo el autor puede editar esta publicación");
        }
        String title = post.getTitle() == null
            ? null : requireTitle(request.title());
        post.updateContent(title, request.content().trim());
        return toResponse(post, membership);
    }

    @Transactional
    public void delete(Long communityId, Long postId, String email) {
        CommunityMembership membership = requireMembership(communityId, email);
        CommunityPost post = requirePost(communityId, postId);
        boolean author = post.getAuthor().getId().equals(membership.getUser().getId());
        if (!author && !MODERATION_ROLES.contains(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permisos para eliminar esta publicación");
        }
        postRepository.delete(post);
    }

    private CommunityMembership requireMembership(Long communityId, String email) {
        return membershipRepository.findByCommunityIdAndUserEmailIgnoreCase(
                communityId, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Grupo no encontrado"));
    }

    private CommunityPost requirePost(Long communityId, Long postId) {
        return postRepository.findById(postId)
            .filter(post -> post.getCommunity().getId().equals(communityId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Publicación no encontrada"));
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La publicación principal debe tener un título");
        }
        return title.trim();
    }

    private CommunityPostResponse toResponse(CommunityPost post,
                                             CommunityMembership viewer) {
        boolean author = post.getAuthor().getId().equals(viewer.getUser().getId());
        return new CommunityPostResponse(
            post.getId(), post.getAuthor().getId(), post.getAuthor().getName(),
            post.getAuthor().hasAvatar(), post.getTitle(), post.getContent(),
            post.getCreatedAt(),
            post.getUpdatedAt(), author,
            author || MODERATION_ROLES.contains(viewer.getRole()));
    }
}
