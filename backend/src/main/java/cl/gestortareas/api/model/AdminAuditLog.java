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
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false, length = 255)
    private String actorEmail;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "target_email", nullable = false, length = 255)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminAuditAction action;

    @Column(name = "previous_value", nullable = false, length = 100)
    private String previousValue;

    @Column(name = "new_value", nullable = false, length = 100)
    private String newValue;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AdminAuditLog() {
    }

    public AdminAuditLog(
        String actorEmail,
        AppUser target,
        AdminAuditAction action,
        String previousValue,
        String newValue,
        String reason
    ) {
        this.actorEmail = actorEmail;
        this.targetUserId = target.getId();
        this.targetEmail = target.getEmail();
        this.action = action;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getActorEmail() { return actorEmail; }
    public Long getTargetUserId() { return targetUserId; }
    public String getTargetEmail() { return targetEmail; }
    public AdminAuditAction getAction() { return action; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
