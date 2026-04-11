create table if not exists products (
    id bigserial primary key,
    product_no varchar(64) not null unique,
    name varchar(120) not null,
    category_name varchar(100) not null,
    price numeric(12, 2) not null,
    stock integer not null,
    status varchar(32) not null,
    description varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_products_category_name on products (category_name);
create index if not exists idx_products_status on products (status);
