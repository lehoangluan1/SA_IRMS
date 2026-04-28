-- Rename the deprecated DB fanout compatibility table so it cannot be confused with the canonical RabbitMQ outbox pipeline.
-- The active asynchronous integration path is: outbox_events -> RabbitMQ relay -> processed_events/inbox consumers.

do $$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'outbox_event_deliveries'
    ) and not exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'legacy_outbox_event_deliveries_deprecated'
    ) then
        alter table outbox_event_deliveries
            rename to legacy_outbox_event_deliveries_deprecated;
    end if;
end $$;

alter index if exists idx_outbox_event_deliveries_pending
    rename to idx_legacy_outbox_event_deliveries_pending;

alter index if exists idx_outbox_event_deliveries_event
    rename to idx_legacy_outbox_event_deliveries_event;

comment on table legacy_outbox_event_deliveries_deprecated is
    'DEPRECATED compatibility artifact only. Not used by the canonical RabbitMQ transactional outbox relay or inbox consumers.';

comment on column legacy_outbox_event_deliveries_deprecated.status is
    'Legacy compatibility status only. Canonical publish state lives on outbox_events and canonical consumer state lives on processed_events.';
