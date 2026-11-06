package cl.gestortareas.api.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.CommunityDetailResponse;
import cl.gestortareas.api.dto.CommunityMemberResponse;
import cl.gestortareas.api.dto.CommunitySummaryResponse;
import cl.gestortareas.api.dto.CreateCommunityRequest;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.Community;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;

@Service
public class CommunityService {
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final AppUserRepository userRepository;

    public CommunityService(CommunityRepository communityRepository,
                            CommunityMembershipRepository membershipRepository,
                            AppUserRepository userRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryResponse> findMine(String email) {
        return membershipRepository
            .findAllByUserEmailIgnoreCaseOrderByJoinedAtDesc(email)
            .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public CommunityDetailResponse findById(Long id, String email) {
        CommunityMembership membership = membershipRepository
            .findByCommunityIdAndUserEmailIgnoreCase(id, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Grupo no encontrado"));
        Community community = membership.getCommunity();
        List<CommunityMemberResponse> members = membershipRepository
            .findAllByCommunityIdOrderByJoinedAtAsc(id)
            .stream().map(this::toMember).toList();
        return new CommunityDetailResponse(
            community.getId(), community.getName(), community.getSlug(),
            community.getDescription(), community.getVisibility(),
            membership.getRole(), community.getCreatedAt(), members);
    }

    @Transactional
    public CommunitySummaryResponse create(CreateCommunityRequest request,
                                           String email) {
        AppUser owner = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
        String name = request.name().trim();
        Community community = communityRepository.save(new Community(
            name, uniqueSlug(name), request.description().trim(),
            request.visibility(), owner));
        return toSummary(membershipRepository.save(
            new CommunityMembership(community, owner, CommunityRole.OWNER)));
    }

    private CommunitySummaryResponse toSummary(CommunityMembership membership) {
        Community community = membership.getCommunity();
        return new CommunitySummaryResponse(
            community.getId(), community.getName(), community.getSlug(),
            community.getDescription(), community.getVisibility(),
            membership.getRole(),
            membershipRepository.countByCommunityId(community.getId()),
            community.getCreatedAt());
    }

    private CommunityMemberResponse toMember(CommunityMembership membership) {
        AppUser user = membership.getUser();
        return new CommunityMemberResponse(user.getId(), user.getName(),
            user.getEmail(), membership.getRole(), membership.getJoinedAt());
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "grupo";
        if (base.length() > 105) {
            base = base.substring(0, 105).replaceAll("-$", "");
        }
        String candidate = base;
        int suffix = 2;
        while (communityRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
