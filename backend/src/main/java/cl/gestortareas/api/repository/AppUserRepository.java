package cl.gestortareas.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.UserRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(UserRole role);

    Page<AppUser> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String name,
        String email,
        Pageable pageable
    );
}
