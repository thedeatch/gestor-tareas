package cl.gestortareas.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.RefreshToken;
import cl.gestortareas.api.repository.RefreshTokenRepository;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.dto.UserSessionResponse;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationDays;
    private final AppUserRepository userRepository;

    public RefreshTokenService(
        RefreshTokenRepository repository,
        AppUserRepository userRepository,
        @Value("${security.refresh-token.expiration-days:7}") long expirationDays
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String issue(AppUser user, String userAgent, String ipAddress) {
        repository.deleteByExpiresAtBefore(Instant.now());
        String rawToken = generateRawToken();
        repository.save(new RefreshToken(
            user,
            hash(rawToken),
            Instant.now().plus(expirationDays, ChronoUnit.DAYS),
            truncate(userAgent, 255),
            truncate(ipAddress, 64)
        ));
        return rawToken;
    }

    @Transactional
    public RotatedToken consumeAndRotate(String rawToken) {
        RefreshToken storedToken = repository.findByTokenHash(hash(rawToken))
            .orElseThrow(this::invalidToken);

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }

        AppUser user = storedToken.getUser();
        if (!user.isActive() || !user.isEmailVerified()) {
            throw invalidToken();
        }
        String rotatedToken = generateRawToken();
        storedToken.rotate(
            hash(rotatedToken),
            Instant.now().plus(expirationDays, ChronoUnit.DAYS)
        );
        return new RotatedToken(user, rotatedToken);
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> list(String email, String currentToken) {
        AppUser user = findUser(email);
        String currentHash = hash(currentToken);
        return repository.findAllByUserOrderByLastUsedAtDesc(user).stream()
            .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
            .map(token -> new UserSessionResponse(
                token.getId(), describeDevice(token.getUserAgent()),
                token.getIpAddress(), token.getCreatedAt(),
                token.getLastUsedAt(), token.getExpiresAt(),
                token.hasTokenHash(currentHash)
            ))
            .toList();
    }

    @Transactional
    public boolean revokeSession(String email, Long id, String currentToken) {
        AppUser user = findUser(email);
        RefreshToken token = repository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "La sesión no existe"
            ));
        boolean current = token.hasTokenHash(hash(currentToken));
        repository.delete(token);
        return current;
    }

    @Transactional
    public void revokeOtherSessions(String email, String currentToken) {
        AppUser user = findUser(email);
        String currentHash = hash(currentToken);
        repository.findAllByUserOrderByLastUsedAtDesc(user).stream()
            .filter(token -> !token.hasTokenHash(currentHash))
            .forEach(repository::delete);
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

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(this::invalidToken);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= maximumLength
            ? value : value.substring(0, maximumLength);
    }

    private String describeDevice(String agent) {
        if (agent == null) return "Dispositivo desconocido";
        String browser = agent.contains("Edg/") ? "Microsoft Edge"
            : agent.contains("Firefox/") ? "Firefox"
            : agent.contains("Chrome/") ? "Google Chrome"
            : agent.contains("Safari/") ? "Safari" : "Navegador";
        String system = agent.contains("Windows") ? "Windows"
            : agent.contains("Android") ? "Android"
            : agent.contains("iPhone") || agent.contains("iPad") ? "iOS"
            : agent.contains("Mac OS") ? "macOS"
            : agent.contains("Linux") ? "Linux" : "sistema desconocido";
        return browser + " en " + system;
    }

    public record RotatedToken(AppUser user, String rawToken) {
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "La sesión ya no puede renovarse"
        );
    }
}
