alter table outbox_events
drop constraint if exists chk_outbox_events_status;

alter table outbox_events
add constraint chk_outbox_events_status
check (status in ('PENDING', 'SENT', 'FAILED', 'DEAD'));
