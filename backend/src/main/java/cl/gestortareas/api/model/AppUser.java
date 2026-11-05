package cl.gestortareas.api.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "avatar_data")
    private byte[] avatarData;

    @Column(name = "avatar_content_type", length = 50)
    private String avatarContentType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_file_id")
    private StoredFile avatarFile;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false, length = 10)
    private ThemePreference themePreference = ThemePreference.LIGHT;

    @Column(name = "reduced_motion", nullable = false)
    private boolean reducedMotion;

    protected AppUser() {
    }

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public byte[] getAvatarData() {
        return avatarData;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public boolean hasAvatar() {
        return avatarFile != null || (avatarData != null && avatarData.length > 0);
    }

    public StoredFile getAvatarFile() { return avatarFile; }

    public StoredFile updateAvatarFile(StoredFile file) {
        StoredFile previous = avatarFile;
        avatarFile = file;
        avatarData = null;
        avatarContentType = null;
        return previous;
    }

    public void updateAvatar(byte[] data, String contentType) {
        this.avatarData = data;
        this.avatarContentType = contentType;
    }

    public void removeAvatar() {
        avatarFile = null;
        this.avatarData = null;
        this.avatarContentType = null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void changeActiveStatus(boolean active) {
        this.active = active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void requireEmailVerification() {
        this.emailVerified = false;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public boolean isLoginLockedAt(Instant instant) {
        return lockedUntil != null && lockedUntil.isAfter(instant);
    }

    public void clearExpiredLoginLockAt(Instant instant) {
        if (lockedUntil != null && !lockedUntil.isAfter(instant)) {
            clearLoginFailures();
        }
    }

    public void registerFailedLogin(
        int maximumAttempts,
        long lockMinutes,
        Instant instant
    ) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumAttempts) {
            lockedUntil = instant.plusSeconds(lockMinutes * 60);
        }
    }

    public void clearLoginFailures() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public ThemePreference getThemePreference() {
        return themePreference;
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public void updatePreferences(
        ThemePreference themePreference,
        boolean reducedMotion
    ) {
        this.themePreference = themePreference;
        this.reducedMotion = reducedMotion;
    }
}
