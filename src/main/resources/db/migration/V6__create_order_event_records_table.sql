create table if not exists order_event_records (
    id bigserial primary key,
    event_type varchar(64) not null,
    order_no varchar(64) not null,
    processed_at timestamp with time zone not null
);

create index if not exists idx_order_event_records_order_no on order_event_records (order_no);
