CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    user_name  VARCHAR(50) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    chat_id    BIGINT UNIQUE,
    google_id  VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_chat_id ON users (chat_id);
CREATE INDEX idx_users_google_id ON users (google_id);

-- ========================================
-- INCOMES TABLE
-- ========================================
CREATE TABLE incomes
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    amount     NUMERIC(15, 2) NOT NULL,
    category   VARCHAR(50) NOT NULL,
    source     VARCHAR(100) NOT NULL,
    date       TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incomes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_incomes_user_id ON incomes (user_id);
CREATE INDEX idx_incomes_date ON incomes (date);
CREATE INDEX idx_incomes_category ON incomes (category);

-- ========================================
-- EXPENSES TABLE
-- ========================================
CREATE TABLE expenses
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    amount     NUMERIC(15, 2) NOT NULL,
    category   VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    date       TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_date ON expenses (date);
CREATE INDEX idx_expenses_category ON expenses (category);
