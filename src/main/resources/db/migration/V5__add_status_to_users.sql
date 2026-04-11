alter table users
    add column if not exists status varchar(32) not null default 'ACTIVE';
