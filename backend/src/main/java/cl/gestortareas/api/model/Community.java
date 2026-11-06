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
@Table(name = "communities")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 3000)
    private String extendedDescription = "";

    @Column(nullable = false, length = 5000)
    private String rules = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityVisibility visibility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Community() {}

    public Community(String name, String slug, String description,
                     CommunityVisibility visibility, AppUser createdBy) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.visibility = visibility;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getExtendedDescription() { return extendedDescription; }
    public String getRules() { return rules; }

    public void updateGuidelines(String extendedDescription, String rules) {
        this.extendedDescription = extendedDescription;
        this.rules = rules;
    }

    public CommunityVisibility getVisibility() { return visibility; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
