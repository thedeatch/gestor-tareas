package cl.gestortareas.api.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.AuthRequest;
import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.RegisterRequest;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        userRepository.save(user);

        return new AuthResponse(
            jwtService.generateToken(user),
            email,
            user.getName(),
            user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return new AuthResponse(
            jwtService.generateToken(user),
            email,
            user.getName(),
            user.getRole()
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
