alter table order_event_records
add constraint uk_order_event_records_event_type_order_no
unique (event_type, order_no);
