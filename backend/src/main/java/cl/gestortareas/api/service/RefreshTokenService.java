package cl.gestortareas.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.RefreshToken;
import cl.gestortareas.api.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationDays;

    public RefreshTokenService(
        RefreshTokenRepository repository,
        @Value("${security.refresh-token.expiration-days:7}") long expirationDays
    ) {
        this.repository = repository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String issue(AppUser user) {
        repository.deleteByExpiresAtBefore(Instant.now());
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(bytes);
        repository.save(new RefreshToken(
            user,
            hash(rawToken),
            Instant.now().plus(expirationDays, ChronoUnit.DAYS)
        ));
        return rawToken;
    }

    @Transactional
    public AppUser consume(String rawToken) {
        RefreshToken storedToken = repository.findByTokenHash(hash(rawToken))
            .orElseThrow(this::invalidToken);
        repository.delete(storedToken);

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }

        AppUser user = storedToken.getUser();
        if (!user.isActive() || !user.isEmailVerified()) {
            throw invalidToken();
        }
        return user;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            repository.deleteByTokenHash(hash(rawToken));
        }
    }

    @Transactional
    public void revokeAll(AppUser user) {
        repository.deleteAllByUser(user);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "La sesión ya no puede renovarse"
        );
    }
}
