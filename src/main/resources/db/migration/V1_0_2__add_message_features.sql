-- Добавляем новые колонки для расширенной функциональности сообщений
ALTER TABLE events.event_messages
    ADD COLUMN IF NOT EXISTS is_edited BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS read_by JSONB DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS parent_message_id BIGINT,
    ADD COLUMN IF NOT EXISTS reactions JSONB DEFAULT '{}'::jsonb;

-- Добавляем внешний ключ для parent_message_id
ALTER TABLE events.event_messages
    ADD CONSTRAINT fk_parent_message
    FOREIGN KEY (parent_message_id)
    REFERENCES events.event_messages(message_id)
    ON DELETE SET NULL; 