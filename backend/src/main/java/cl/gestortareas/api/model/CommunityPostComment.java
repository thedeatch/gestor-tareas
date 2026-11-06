package cl.gestortareas.api.model;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "community_post_comments")
public class CommunityPostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id")
    private AppUser author;

    @Column(name = "parent_id")
    private Long parentId;

    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    protected CommunityPostComment() {}

    public CommunityPostComment(CommunityPost post, AppUser author, Long parentId, String content) {
        this.post = post;
        this.author = author;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public AppUser getAuthor() { return author; }
    public Long getParentId() { return parentId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void edit(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }
}