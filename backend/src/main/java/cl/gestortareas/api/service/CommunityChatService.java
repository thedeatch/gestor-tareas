package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityChatService {
    public record Message(Long id, String authorName, String content, OffsetDateTime createdAt) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityChatService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Message> messages(Long groupId, String email, Long after) {
        communities.requireMembership(groupId, email);
        if (after == null) {
            return jdbc.sql("""
                SELECT id, author_name, content, created_at FROM (
                    SELECT m.id, u.name AS author_name, m.content, m.created_at
                    FROM community_chat_messages m LEFT JOIN users u ON u.id = m.author_id
                    WHERE m.community_id = :groupId ORDER BY m.id DESC LIMIT 50
                ) recent ORDER BY id
                """).param("groupId", groupId).query(Message.class).list();
        }
        return jdbc.sql("""
            SELECT m.id, u.name AS author_name, m.content, m.created_at
            FROM community_chat_messages m LEFT JOIN users u ON u.id = m.author_id
            WHERE m.community_id = :groupId AND m.id > :after ORDER BY m.id LIMIT 50
            """).param("groupId", groupId).param("after", after).query(Message.class).list();
    }

    @Transactional
    public void send(Long groupId, String email, String content) {
        var member = communities.requireMembership(groupId, email);
        // Serialize writes within the group so a committed cursor cannot skip a late commit.
        jdbc.sql("SELECT id FROM communities WHERE id = :id FOR UPDATE")
            .param("id", groupId).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO community_chat_messages (community_id, author_id, content)
            VALUES (:groupId, :authorId, :content)
            """).param("groupId", groupId).param("authorId", member.getUser().getId())
            .param("content", content.strip()).update();
    }
}