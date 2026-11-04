package cl.gestortareas.api.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import cl.gestortareas.api.dto.AuthRequest;
import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.AuthSession;
import cl.gestortareas.api.dto.ForgotPasswordRequest;
import cl.gestortareas.api.dto.RegisterRequest;
import cl.gestortareas.api.dto.RegistrationResponse;
import cl.gestortareas.api.dto.ResetPasswordRequest;
import cl.gestortareas.api.dto.VerifyEmailRequest;
import cl.gestortareas.api.dto.UserSessionResponse;
import cl.gestortareas.api.service.AuthService;
import cl.gestortareas.api.service.EmailVerificationService;
import cl.gestortareas.api.service.PasswordResetService;
import cl.gestortareas.api.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final boolean secureRefreshCookie;
    private final long refreshExpirationDays;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
        AuthService authService,
        PasswordResetService passwordResetService,
        EmailVerificationService emailVerificationService,
        RefreshTokenService refreshTokenService,
        @Value("${security.refresh-token.cookie-secure:false}")
        boolean secureRefreshCookie,
        @Value("${security.refresh-token.expiration-days:7}")
        long refreshExpirationDays
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.secureRefreshCookie = secureRefreshCookie;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
        @Valid @RequestBody AuthRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        return writeSession(authService.login(
            request,
            servletRequest.getHeader("User-Agent"),
            clientIp(servletRequest)
        ), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
        @CookieValue(name = "refresh_token", required = false)
        String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No existe una sesión renovable"
            );
        }
        return writeSession(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = "refresh_token", required = false)
        String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshCookie("", Duration.ZERO).toString()
        );
    }

    @GetMapping("/sessions")
    public List<UserSessionResponse> sessions(
        @AuthenticationPrincipal Jwt jwt,
        @CookieValue(name = "refresh_token") String refreshToken
    ) {
        return refreshTokenService.list(jwt.getSubject(), refreshToken);
    }

    @DeleteMapping("/sessions/others")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeOtherSessions(
        @AuthenticationPrincipal Jwt jwt,
        @CookieValue(name = "refresh_token") String refreshToken
    ) {
        refreshTokenService.revokeOtherSessions(jwt.getSubject(), refreshToken);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @CookieValue(name = "refresh_token") String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshTokenService.revokeSession(jwt.getSubject(), id, refreshToken)) {
            response.addHeader(HttpHeaders.SET_COOKIE,
                refreshCookie("", Duration.ZERO).toString());
        }
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestReset(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        emailVerificationService.resend(request.email());
    }

    private AuthResponse writeSession(
        AuthSession session,
        HttpServletResponse response
    ) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshCookie(
                session.refreshToken(),
                Duration.ofDays(refreshExpirationDays)
            ).toString()
        );
        return session.response();
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
            .httpOnly(true)
            .secure(secureRefreshCookie)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(maxAge)
            .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
            ? request.getRemoteAddr()
            : forwarded.split(",")[0].trim();
    }
}
