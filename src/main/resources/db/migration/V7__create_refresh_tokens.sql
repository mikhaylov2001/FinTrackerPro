create table if not exists refresh_tokens (
                                              id varchar(255) primary key,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    revoked_at timestamptz null,
    token_hash varchar(128) not null,
    user_id bigint not null references users(id)
    );

create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);
