alter table orders
add column if not exists product_id bigint;

alter table orders
add column if not exists quantity integer;
