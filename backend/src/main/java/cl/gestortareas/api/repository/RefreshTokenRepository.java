package cl.gestortareas.api.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import cl.gestortareas.api.model.RefreshToken;
import cl.gestortareas.api.model.AppUser;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :hash")
    Optional<RefreshToken> findForUpdateByTokenHash(@Param("hash") String hash);

    void deleteByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant instant);

    void deleteAllByUser(AppUser user);

    List<RefreshToken> findAllByUserOrderByLastUsedAtDesc(AppUser user);

    Optional<RefreshToken> findByIdAndUser(Long id, AppUser user);
}
