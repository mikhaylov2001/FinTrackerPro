CREATE TABLE password_reset_tokens (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT NOT NULL,
                                       token_hash VARCHAR(64) NOT NULL UNIQUE,
                                       expires_at TIMESTAMP NOT NULL,
                                       used_at TIMESTAMP,
                                       created_at TIMESTAMP NOT NULL,
                                       CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token_hash ON password_reset_tokens(token_hash);
