package cl.gestortareas.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.CommunityMembership;

public interface CommunityMembershipRepository
    extends JpaRepository<CommunityMembership, Long> {

    List<CommunityMembership> findAllByUserEmailIgnoreCaseOrderByJoinedAtDesc(
        String email
    );

    List<CommunityMembership> findAllByCommunityIdOrderByJoinedAtAsc(Long id);

    Optional<CommunityMembership> findByCommunityIdAndUserEmailIgnoreCase(
        Long communityId,
        String email
    );

    long countByCommunityId(Long communityId);
}
