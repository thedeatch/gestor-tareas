package cl.gestortareas.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.CommunityPost;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findAllByCommunityIdOrderByCreatedAtDesc(Long communityId);
    boolean existsByCommunityId(Long communityId);
}
