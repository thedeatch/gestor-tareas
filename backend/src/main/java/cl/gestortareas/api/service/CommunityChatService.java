package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.sql.Types;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityChatService {
    public record Channel(Long id, String name) {}
    public record Message(Long id, String authorName, String content, OffsetDateTime createdAt) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunityChatService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public List<Channel> channels(Long groupId, String email) {
        communities.requireMembership(groupId, email);
        return jdbc.sql("""
            SELECT id, name FROM community_chat_channels WHERE community_id = :groupId
            ORDER BY name_key, id
            """).param("groupId", groupId).query(Channel.class).list();
    }

    @Transactional
    public void createChannel(Long groupId, String email, String name) {
        communities.requireManager(groupId, email);
        String normalized = name.strip();
        String key = normalized.toLowerCase(Locale.ROOT);
        if (key.equals("general")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "General es el canal predeterminado");
        }
        lockGroup(groupId);
        int inserted = jdbc.sql("""
            INSERT INTO community_chat_channels (community_id, name, name_key)
            SELECT :groupId, :name, :key WHERE NOT EXISTS (
                SELECT 1 FROM community_chat_channels WHERE community_id = :groupId AND name_key = :key
            )
            """).param("groupId", groupId).param("name", normalized).param("key", key).update();
        if (inserted == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un canal con ese nombre");
        }
    }

    @Transactional(readOnly = true)
    public List<Message> messages(Long groupId, Long channelId, String email, Long after) {
        Long viewer = communities.requireMembership(groupId, email).getUser().getId();
        requireChannel(groupId, channelId);
        if (after == null) {
            return jdbc.sql("""
                SELECT id, author_name, content, created_at FROM (
                    SELECT m.id, u.name AS author_name, m.content, m.created_at
                    FROM community_chat_messages m LEFT JOIN users u ON u.id = m.author_id
                    WHERE m.community_id = :groupId AND m.channel_id IS NOT DISTINCT FROM :channelId
                    AND NOT EXISTS (
                        SELECT 1 FROM user_blocks b
                        WHERE (b.blocker_id = :viewer AND b.blocked_id = m.author_id)
                            OR (b.blocked_id = :viewer AND b.blocker_id = m.author_id)
                    )
                    ORDER BY m.id DESC LIMIT 50
                ) recent ORDER BY id
                """).param("groupId", groupId).param("channelId", channelId, Types.BIGINT).param("viewer", viewer).query(Message.class).list();
        }
        return jdbc.sql("""
            SELECT m.id, u.name AS author_name, m.content, m.created_at
            FROM community_chat_messages m LEFT JOIN users u ON u.id = m.author_id
            WHERE m.community_id = :groupId AND m.channel_id IS NOT DISTINCT FROM :channelId AND m.id > :after
                AND NOT EXISTS (
                    SELECT 1 FROM user_blocks b
                    WHERE (b.blocker_id = :viewer AND b.blocked_id = m.author_id)
                        OR (b.blocked_id = :viewer AND b.blocker_id = m.author_id)
                )
            ORDER BY m.id LIMIT 50
            """).param("groupId", groupId).param("channelId", channelId, Types.BIGINT).param("after", after).param("viewer", viewer).query(Message.class).list();
    }

    @Transactional
    public void send(Long groupId, Long channelId, String email, String content) {
        var member = communities.requireMembership(groupId, email);
        // Serialize writes within the group so a committed cursor cannot skip a late commit.
        lockGroup(groupId);
        requireChannel(groupId, channelId);
        jdbc.sql("""
            INSERT INTO community_chat_messages (community_id, channel_id, author_id, content)
            VALUES (:groupId, :channelId, :authorId, :content)
            """).param("groupId", groupId).param("channelId", channelId, Types.BIGINT)
            .param("authorId", member.getUser().getId()).param("content", content.strip()).update();
    }

    private void lockGroup(Long groupId) {
        jdbc.sql("SELECT id FROM communities WHERE id = :id FOR UPDATE")
            .param("id", groupId).query(Long.class).single();
    }

    private void requireChannel(Long groupId, Long channelId) {
        if (channelId == null) return;
        jdbc.sql("""
            SELECT id FROM community_chat_channels WHERE community_id = :groupId AND id = :channelId
            """).param("groupId", groupId).param("channelId", channelId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Canal no encontrado"));
    }
}