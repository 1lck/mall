create table if not exists orders (
    id bigserial primary key,
    order_no varchar(64) not null unique,
    user_id bigint not null,
    total_amount numeric(12, 2) not null,
    status varchar(32) not null,
    remark varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_orders_user_id on orders (user_id);
create index if not exists idx_orders_status on orders (status);
