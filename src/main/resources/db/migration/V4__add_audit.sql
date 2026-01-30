-- Таблица аудита для всех изменений
CREATE TABLE audit_log
(
    id          BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    user_id     BIGINT,
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_user   ON audit_log (user_id);
CREATE INDEX idx_audit_date   ON audit_log (changed_at DESC);

CREATE TABLE error_log
(
    id            BIGSERIAL PRIMARY KEY,
    error_code    VARCHAR(50),
    error_message TEXT,
    stack_trace   TEXT,
    user_id       BIGINT,
    endpoint      VARCHAR(255),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_error_date ON error_log (occurred_at DESC);
CREATE INDEX idx_error_code ON error_log (error_code);