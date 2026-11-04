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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

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
        return avatarData != null && avatarData.length > 0;
    }

    public void updateAvatar(byte[] data, String contentType) {
        this.avatarData = data;
        this.avatarContentType = contentType;
    }

    public void removeAvatar() {
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
}
