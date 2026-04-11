create table if not exists users (
    id bigserial primary key,
    username varchar(50) not null unique,
    nickname varchar(80) not null,
    password_hash varchar(120) not null,
    role varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_users_username on users (username);
