package cl.gestortareas.api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.Community;
import cl.gestortareas.api.model.CommunityVisibility;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    boolean existsBySlug(String slug);

    List<Community> findAllByVisibilityInOrderByCreatedAtDesc(
        Collection<CommunityVisibility> visibilities);
}
