-- Индекс для быстрого поиска сообщений по event_id и sent_at
CREATE INDEX IF NOT EXISTS idx_event_messages_event_sent ON events.event_messages(event_id, sent_at);

-- Индекс для быстрого поиска сообщений по sender_id
CREATE INDEX IF NOT EXISTS idx_event_messages_sender ON events.event_messages(sender_id);
 
-- Индекс для поиска по содержимому сообщения
CREATE INDEX IF NOT EXISTS idx_event_messages_content ON events.event_messages USING gin(to_tsvector('russian', content)); 