package cl.gestortareas.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.EmailVerificationToken;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUser(AppUser user);

    void deleteByUser(AppUser user);
}
