ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name varchar(255),
    ADD COLUMN IF NOT EXISTS last_name varchar(255);
