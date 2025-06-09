CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE IF NOT EXISTS users.user_fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    device_type VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_fcm_tokens_user_id FOREIGN KEY (user_id) REFERENCES users.users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_fcm_tokens_user_id ON users.user_fcm_tokens(user_id);
CREATE INDEX idx_user_fcm_tokens_token ON users.user_fcm_tokens(token); 