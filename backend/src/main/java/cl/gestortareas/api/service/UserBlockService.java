package cl.gestortareas.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class UserBlockService {
    public record BlockedUser(Long id, String name) {}
    public record Status(boolean canBlock, boolean blocked) {}

    private final JdbcClient jdbc;
    private final AppUserRepository users;

    public UserBlockService(JdbcClient jdbc, AppUserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Status status(String email, Long target) {
        Long actor = actorId(email);
        return new Status(!actor.equals(target), jdbc.sql("""
            SELECT COUNT(*) FROM user_blocks WHERE blocker_id = :actor AND blocked_id = :target
            """).param("actor", actor).param("target", target).query(Long.class).single() > 0);
    }

    @Transactional(readOnly = true)
    public List<BlockedUser> list(String email) {
        return jdbc.sql("""
            SELECT u.id, u.name FROM user_blocks b JOIN users u ON u.id = b.blocked_id
            WHERE b.blocker_id = :actor ORDER BY u.name, u.id
            """).param("actor", actorId(email)).query(BlockedUser.class).list();
    }

    public boolean between(Long first, Long second) {
        return jdbc.sql("""
            SELECT COUNT(*) FROM user_blocks
            WHERE (blocker_id = :first AND blocked_id = :second)
                OR (blocker_id = :second AND blocked_id = :first)
            """).param("first", first).param("second", second).query(Long.class).single() > 0;
    }

    @Transactional
    public void set(String email, Long target, boolean blocked) {
        Long actor = actorId(email);
        if (actor.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes bloquear tu propia cuenta");
        }
        // Lock both users in the same order, including when they block each other concurrently.
        var locked = jdbc.sql("SELECT id FROM users WHERE id IN (:actor, :target) ORDER BY id FOR UPDATE")
            .param("actor", actor).param("target", target).query(Long.class).list();
        if (blocked) {
            if (!locked.contains(target)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
            }
            jdbc.sql("""
                INSERT INTO user_blocks (blocker_id, blocked_id)
                SELECT :actor, :target WHERE NOT EXISTS (
                    SELECT 1 FROM user_blocks WHERE blocker_id = :actor AND blocked_id = :target
                )
                """).param("actor", actor).param("target", target).update();
        } else {
            jdbc.sql("DELETE FROM user_blocks WHERE blocker_id = :actor AND blocked_id = :target")
                .param("actor", actor).param("target", target).update();
        }
    }

    private Long actorId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")).getId();
    }
}