package cl.gestortareas.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.AuthRequest;
import cl.gestortareas.api.dto.AuthResponse;
import cl.gestortareas.api.dto.ForgotPasswordRequest;
import cl.gestortareas.api.dto.RegisterRequest;
import cl.gestortareas.api.dto.RegistrationResponse;
import cl.gestortareas.api.dto.ResetPasswordRequest;
import cl.gestortareas.api.dto.VerifyEmailRequest;
import cl.gestortareas.api.service.AuthService;
import cl.gestortareas.api.service.EmailVerificationService;
import cl.gestortareas.api.service.PasswordResetService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
        AuthService authService,
        PasswordResetService passwordResetService,
        EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
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
}
