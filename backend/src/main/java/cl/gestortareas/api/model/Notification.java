package cl.gestortareas.api.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(
        AppUser recipient,
        AppUser actor,
        NotificationType type,
        String title,
        String message,
        String targetUrl
    ) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public AppUser getActor() { return actor; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return readAt != null; }

    public void markAsRead() {
        if (readAt == null) readAt = Instant.now();
    }
}
