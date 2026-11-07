package cl.gestortareas.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class PrivateChatService {
    public record Message(Long id, String authorName, String content, OffsetDateTime createdAt) {}
    public record Conversation(Long userId, String name, OffsetDateTime lastMessageAt) {}

    private final JdbcClient jdbc;
    private final AppUserRepository users;
    private final UserBlockService blocks;

    public PrivateChatService(JdbcClient jdbc, AppUserRepository users, UserBlockService blocks) {
        this.jdbc = jdbc;
        this.users = users;
        this.blocks = blocks;
    }

    @Transactional(readOnly = true)
    public List<Conversation> conversations(String email) {
        Long viewer = requireUser(email).getId();
        return jdbc.sql("""
            SELECT u.id AS user_id, u.name, MAX(m.created_at) AS last_message_at
            FROM private_chat_messages m JOIN users u
                ON u.id = CASE WHEN m.sender_id = :viewer THEN m.recipient_id ELSE m.sender_id END
            WHERE (m.sender_id = :viewer OR m.recipient_id = :viewer) AND u.active = TRUE
                AND NOT EXISTS (
                    SELECT 1 FROM user_blocks b
                    WHERE (b.blocker_id = :viewer AND b.blocked_id = u.id)
                        OR (b.blocked_id = :viewer AND b.blocker_id = u.id)
                )
            GROUP BY u.id, u.name ORDER BY last_message_at DESC, u.id
            """).param("viewer", viewer).query(Conversation.class).list();
    }

    @Transactional(readOnly = true)
    public List<Message> messages(String email, Long peerId, Long after) {
        Long viewer = requireUser(email).getId();
        requireAvailable(viewer, peerId);
        if (after == null) {
            return jdbc.sql("""
                SELECT id, author_name, content, created_at FROM (
                    SELECT m.id, u.name AS author_name, m.content, m.created_at
                    FROM private_chat_messages m JOIN users u ON u.id = m.sender_id
                    WHERE (m.sender_id = :viewer AND m.recipient_id = :peer)
                        OR (m.sender_id = :peer AND m.recipient_id = :viewer)
                    ORDER BY m.id DESC LIMIT 50
                ) recent ORDER BY id
                """).param("viewer", viewer).param("peer", peerId).query(Message.class).list();
        }
        return jdbc.sql("""
            SELECT m.id, u.name AS author_name, m.content, m.created_at
            FROM private_chat_messages m JOIN users u ON u.id = m.sender_id
            WHERE m.id > :after AND (
                (m.sender_id = :viewer AND m.recipient_id = :peer)
                OR (m.sender_id = :peer AND m.recipient_id = :viewer)
            ) ORDER BY m.id LIMIT 50
            """).param("viewer", viewer).param("peer", peerId).param("after", after)
            .query(Message.class).list();
    }

    @Transactional
    public void send(String email, Long peerId, String content) {
        Long viewer = requireUser(email).getId();
        // Match the block service's lock order and serialize message cursors for the pair.
        jdbc.sql("SELECT id FROM users WHERE id IN (:viewer, :peer) ORDER BY id FOR UPDATE")
            .param("viewer", viewer).param("peer", peerId).query(Long.class).list();
        requireAvailable(viewer, peerId);
        jdbc.sql("""
            INSERT INTO private_chat_messages (sender_id, recipient_id, content)
            VALUES (:viewer, :peer, :content)
            """).param("viewer", viewer).param("peer", peerId).param("content", content.strip()).update();
    }

    private void requireAvailable(Long viewer, Long peerId) {
        if (viewer.equals(peerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona otro usuario");
        }
        var peer = users.findById(peerId).filter(AppUser::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no disponible"));
        if (blocks.between(viewer, peerId) || (!peer.isPublicProfile() && !hasHistory(viewer, peerId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversación no disponible");
        }
    }

    private boolean hasHistory(Long viewer, Long peer) {
        return jdbc.sql("""
            SELECT id FROM private_chat_messages
            WHERE (sender_id = :viewer AND recipient_id = :peer)
                OR (sender_id = :peer AND recipient_id = :viewer) LIMIT 1
            """).param("viewer", viewer).param("peer", peer).query(Long.class).optional().isPresent();
    }

    private AppUser requireUser(String email) {
        return users.findByEmailIgnoreCase(email).filter(AppUser::isActive).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no disponible"));
    }
}