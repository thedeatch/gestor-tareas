package cl.gestortareas.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.EmailVerificationToken;
import cl.gestortareas.api.repository.EmailVerificationTokenRepository;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationMailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
        EmailVerificationTokenRepository tokenRepository,
        EmailVerificationMailService mailService
    ) {
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void issueFor(AppUser user) {
        tokenRepository.deleteByUser(user);
        String rawToken = generateToken();
        tokenRepository.save(new EmailVerificationToken(
            user,
            hash(rawToken),
            Instant.now().plus(Duration.ofHours(24))
        ));
        mailService.sendVerification(user.getEmail(), rawToken);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(this::invalidToken);
        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw invalidToken();
        }
        token.getUser().verifyEmail();
        tokenRepository.deleteByUser(token.getUser());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El enlace de verificación no es válido o expiró"
        );
    }
}
