package cl.gestortareas.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
public class NotificationPreferenceService {
    private final JdbcClient jdbc;
    private final AppUserRepository users;

    public NotificationPreferenceService(JdbcClient jdbc, AppUserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NotificationType> disabled(String email) {
        return jdbc.sql("SELECT type FROM disabled_notification_types WHERE user_id = :id ORDER BY type")
            .param("id", userId(email)).query((rs, row) -> NotificationType.valueOf(rs.getString("type"))).list();
    }

    public boolean isDisabled(Long userId, NotificationType type) {
        return configurable(type) && jdbc.sql("""
            SELECT 1 FROM disabled_notification_types WHERE user_id = :id AND type = :type
            """).param("id", userId).param("type", type.name()).query(Integer.class).optional().isPresent();
    }

    @Transactional
    public void set(String email, NotificationType type, boolean enabled) {
        if (!configurable(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este tipo de aviso siempre está activo");
        }
        Long userId = userId(email);
        jdbc.sql("SELECT id FROM users WHERE id = :id FOR UPDATE")
            .param("id", userId).query(Long.class).single();
        if (enabled) {
            jdbc.sql("DELETE FROM disabled_notification_types WHERE user_id = :id AND type = :type")
                .param("id", userId).param("type", type.name()).update();
        } else {
            jdbc.sql("""
                INSERT INTO disabled_notification_types (user_id, type)
                SELECT :id, :type WHERE NOT EXISTS (
                    SELECT 1 FROM disabled_notification_types WHERE user_id = :id AND type = :type
                )
                """).param("id", userId).param("type", type.name()).update();
        }
    }

    private boolean configurable(NotificationType type) {
        return switch (type) {
            case INVITATION, TASK, COMMENT, REACTION, EVENT -> true;
            case SYSTEM, MODERATION -> false;
        };
    }

    private Long userId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")).getId();
    }
}