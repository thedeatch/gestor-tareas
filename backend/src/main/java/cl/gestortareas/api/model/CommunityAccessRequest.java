package cl.gestortareas.api.model;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "community_access_requests")
public class CommunityAccessRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityAccessType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityAccessStatus status = CommunityAccessStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private AppUser resolvedBy;

    protected CommunityAccessRequest() {}

    public CommunityAccessRequest(Community community, AppUser user,
                                  AppUser createdBy,
                                  CommunityAccessType type) {
        this.community = community;
        this.user = user;
        this.createdBy = createdBy;
        this.type = type;
    }

    public Long getId() { return id; }
    public Community getCommunity() { return community; }
    public AppUser getUser() { return user; }
    public AppUser getCreatedBy() { return createdBy; }
    public CommunityAccessType getType() { return type; }
    public CommunityAccessStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void reopen(AppUser actor) {
        status = CommunityAccessStatus.PENDING;
        createdBy = actor;
        createdAt = Instant.now();
        resolvedAt = null;
        resolvedBy = null;
    }

    public void resolve(CommunityAccessStatus newStatus, AppUser actor) {
        status = newStatus;
        resolvedAt = Instant.now();
        resolvedBy = actor;
    }
}
