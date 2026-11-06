package cl.gestortareas.api.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.CommunityPost;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findAllByCommunityIdOrderByCreatedAtDesc(Long communityId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommunityPost> findByIdAndCommunityId(Long id, Long communityId);

    boolean existsByCommunityId(Long communityId);
}
