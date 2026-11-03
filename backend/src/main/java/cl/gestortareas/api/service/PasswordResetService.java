package cl.gestortareas.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.ForgotPasswordRequest;
import cl.gestortareas.api.dto.ResetPasswordRequest;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.PasswordResetToken;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.PasswordResetTokenRepository;

@Service
public class PasswordResetService {

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService mailService;
    private final long expirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
        AppUserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        PasswordEncoder passwordEncoder,
        PasswordResetMailService mailService,
        @Value("${app.password-reset.expiration-minutes:15}")
        long expirationMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            tokenRepository.deleteAllByUser(user);

            String rawToken = generateToken();
            PasswordResetToken token = new PasswordResetToken(
                hash(rawToken),
                Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES),
                user
            );
            tokenRepository.save(token);
            mailService.sendPasswordReset(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository
            .findByTokenHash(hash(request.token()))
            .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (!token.isAvailableAt(now)) {
            throw invalidToken();
        }

        AppUser user = token.getUser();
        user.changePassword(passwordEncoder.encode(request.password()));
        token.markAsUsed(now);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                digest.digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El enlace no es válido o ya venció"
        );
    }
}

