package cl.gestortareas.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.PasswordResetToken;

public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(AppUser user);
}

