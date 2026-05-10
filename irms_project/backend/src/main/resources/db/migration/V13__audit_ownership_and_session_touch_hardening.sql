alter table audit_logs
    add column if not exists source_event_id uuid;

create unique index if not exists uq_audit_logs_source_event
    on audit_logs (source_event_id)
    where source_event_id is not null;
