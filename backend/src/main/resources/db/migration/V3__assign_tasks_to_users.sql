ALTER TABLE tasks
    ADD COLUMN user_id BIGINT;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

CREATE INDEX idx_tasks_user_id ON tasks(user_id);

UPDATE tasks
SET user_id = (
    SELECT id
    FROM users
    ORDER BY id
    FETCH FIRST 1 ROW ONLY
)
WHERE user_id IS NULL
  AND EXISTS (SELECT 1 FROM users);
