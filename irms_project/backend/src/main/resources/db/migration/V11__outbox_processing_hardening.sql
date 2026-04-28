create index if not exists idx_outbox_events_pending_order
    on outbox_events (occurred_at, created_at)
    where status = 'pending';

create unique index if not exists uq_notification_messages_outbox_event
    on notification_messages ((payload ->> 'outboxEventId'))
    where payload ? 'outboxEventId';

create unique index if not exists uq_report_snapshots_outbox_event
    on report_snapshots ((payload ->> 'outboxEventId'))
    where payload ? 'outboxEventId';
