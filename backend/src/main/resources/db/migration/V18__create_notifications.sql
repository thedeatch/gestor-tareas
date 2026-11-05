CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_url VARCHAR(500),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread
    ON notifications(recipient_user_id, read_at);

INSERT INTO notifications (
    recipient_user_id,
    type,
    title,
    message,
    target_url
)
SELECT
    id,
    'SYSTEM',
    'Centro de notificaciones disponible',
    'Ya puedes revisar avisos y novedades importantes desde la aplicación.',
    '/notificaciones'
FROM users;
