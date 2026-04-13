create table if not exists outbox_events (
    id bigserial primary key,
    event_id varchar(64) not null,
    aggregate_type varchar(64) not null,
    aggregate_id varchar(128) not null,
    event_type varchar(128) not null,
    topic varchar(255) not null,
    message_key varchar(255) not null,
    payload jsonb not null,
    status varchar(32) not null,
    retry_count integer not null default 0,
    next_retry_at timestamp with time zone null,
    last_error text null,
    sent_at timestamp with time zone null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_outbox_events_event_id unique (event_id),
    constraint chk_outbox_events_status check (status in ('PENDING', 'SENT', 'FAILED'))
);

create index if not exists idx_outbox_events_status_next_retry_at
    on outbox_events (status, next_retry_at, created_at);

create index if not exists idx_outbox_events_aggregate
    on outbox_events (aggregate_type, aggregate_id);
