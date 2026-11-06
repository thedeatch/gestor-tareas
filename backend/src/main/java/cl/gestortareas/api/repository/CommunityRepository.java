package cl.gestortareas.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.Community;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    boolean existsBySlug(String slug);
}
