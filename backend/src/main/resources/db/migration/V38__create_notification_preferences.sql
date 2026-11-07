CREATE TABLE disabled_notification_types (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL CHECK (type IN ('INVITATION', 'TASK', 'COMMENT', 'REACTION', 'EVENT')),
    PRIMARY KEY (user_id, type)
);