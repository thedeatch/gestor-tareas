package cl.gestortareas.api.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.AuthRequest;
import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.AuthSession;
import cl.gestortareas.api.dto.RegisterRequest;
import cl.gestortareas.api.dto.RegistrationResponse;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final boolean emailVerificationEnabled;

    public AuthService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        EmailVerificationService emailVerificationService,
        RefreshTokenService refreshTokenService,
        @Value("${app.email-verification.enabled:true}")
        boolean emailVerificationEnabled
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya está registrado"
            );
        }

        AppUser user = new AppUser(
            request.name().trim(),
            email,
            passwordEncoder.encode(request.password())
        );
        if (emailVerificationEnabled) {
            user.requireEmailVerification();
        }
        userRepository.save(user);
        if (emailVerificationEnabled) {
            emailVerificationService.issueFor(user);
        }
        return new RegistrationResponse(emailVerificationEnabled);
    }

    @Transactional
    public AuthSession login(AuthRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta está suspendida"
            );
        }

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Debes confirmar tu correo antes de iniciar sesión"
            );
        }

        return createSession(user);
    }

    @Transactional
    public AuthSession refresh(String refreshToken) {
        return createSession(refreshTokenService.consume(refreshToken));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthSession createSession(AppUser user) {
        return new AuthSession(
            new AuthResponse(
                jwtService.generateToken(user),
                user.getEmail(),
                user.getName(),
                user.getRole()
            ),
            refreshTokenService.issue(user)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Correo o contraseña incorrectos"
        );
    }
}
