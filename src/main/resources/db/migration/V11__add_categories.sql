CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(50)  NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    is_system  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_user_type_name UNIQUE (user_id, type, name)
);

CREATE INDEX idx_categories_user_type ON categories (user_id, type);
