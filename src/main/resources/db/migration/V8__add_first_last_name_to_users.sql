ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name varchar(255),
    ADD COLUMN IF NOT EXISTS last_name varchar(255);
    ADD COLUMN display_currency VARCHAR(3) NOT NULL DEFAULT 'RUB';
    ADD COLUMN hide_amounts BOOLEAN NOT NULL DEFAULT FALSE;
