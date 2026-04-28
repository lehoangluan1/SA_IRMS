-- Quarantine legacy DB fanout artifacts so they cannot be mistaken for the active event pipeline.
-- RabbitMQ relay + inbox processing is the canonical integration path.

comment on table outbox_event_deliveries is
    'DEPRECATED compatibility artifact only. Not used by the active RabbitMQ outbox relay pipeline.';

comment on column outbox_event_deliveries.status is
    'Legacy compatibility status only. Canonical outbox status semantics live on outbox_events.';
