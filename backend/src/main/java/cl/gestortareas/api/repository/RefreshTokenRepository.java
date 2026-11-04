package cl.gestortareas.api.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.RefreshToken;
import cl.gestortareas.api.model.AppUser;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant instant);

    void deleteAllByUser(AppUser user);

    List<RefreshToken> findAllByUserOrderByLastUsedAtDesc(AppUser user);

    Optional<RefreshToken> findByIdAndUser(Long id, AppUser user);
}
