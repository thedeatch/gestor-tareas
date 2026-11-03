package cl.gestortareas.api.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(
        String tokenHash,
        Instant expiresAt,
        AppUser user
    ) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public AppUser getUser() {
        return user;
    }

    public boolean isAvailableAt(Instant instant) {
        return usedAt == null && expiresAt.isAfter(instant);
    }

    public void markAsUsed(Instant instant) {
        usedAt = instant;
    }
}

