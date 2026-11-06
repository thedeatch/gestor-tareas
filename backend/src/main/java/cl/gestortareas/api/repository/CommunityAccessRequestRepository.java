package cl.gestortareas.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.*;

public interface CommunityAccessRequestRepository
    extends JpaRepository<CommunityAccessRequest, Long> {

    Optional<CommunityAccessRequest> findByCommunityIdAndUserIdAndType(
        Long communityId, Long userId, CommunityAccessType type);

    Optional<CommunityAccessRequest> findByIdAndType(
        Long id, CommunityAccessType type);

    List<CommunityAccessRequest>
        findAllByCommunityIdAndTypeAndStatusOrderByCreatedAtAsc(
            Long communityId, CommunityAccessType type,
            CommunityAccessStatus status);

    List<CommunityAccessRequest>
        findAllByUserEmailIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(
            String email, CommunityAccessType type,
            CommunityAccessStatus status);
}
