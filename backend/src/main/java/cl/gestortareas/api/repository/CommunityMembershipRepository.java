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

    List<CommunityMembership> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    List<CommunityMembership> findAllByCommunityIdOrderByJoinedAtAsc(Long id);

    Optional<CommunityMembership> findByCommunityIdAndUserEmailIgnoreCase(
        Long communityId,
        String email
    );

    Optional<CommunityMembership> findByCommunityIdAndUserId(
        Long communityId,
        Long userId
    );

    boolean existsByCommunityIdAndUserId(Long communityId, Long userId);

    List<CommunityMembership> findAllByCommunityIdAndRoleIn(
        Long communityId,
        List<cl.gestortareas.api.model.CommunityRole> roles
    );

    long countByCommunityId(Long communityId);
}
