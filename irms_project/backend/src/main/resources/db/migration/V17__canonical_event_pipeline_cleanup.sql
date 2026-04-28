-- Canonicalize the async pipeline around RabbitMQ transactional outbox and processed-event inbox.

alter table outbox_events
    drop constraint if exists outbox_events_status_check;

update outbox_events
set status = case lower(status)
    when 'pending' then 'PENDING'
    when 'processed' then 'PUBLISHED'
    when 'failed' then 'FAILED'
    else status
end
where lower(status) in ('pending', 'processed', 'failed');

alter table outbox_events
    add constraint outbox_events_status_check
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'));

update outbox_events
set event_type = coalesce(event_type, type),
    event_version = coalesce(event_version, 1),
    producer_service = coalesce(nullif(producer_service, ''), 'legacy-compatibility-adapter'),
    causation_id = coalesce(nullif(causation_id, ''), correlation_id),
    next_retry_at = coalesce(next_retry_at, occurred_at, created_at, now()),
    created_at = coalesce(created_at, occurred_at, now())
where event_type is null
   or producer_service is null
   or producer_service = ''
   or causation_id is null
   or causation_id = ''
   or next_retry_at is null
   or created_at is null;

do $$
begin
    if to_regclass(format('%I.%I', current_schema(), 'workflow_states')) is not null then
        execute format($sql$
            insert into workflow_instances (
                workflow_id,
                workflow_type,
                aggregate_type,
                aggregate_id,
                state,
                version,
                correlation_id,
                failure_reason,
                last_event_id,
                payload,
                created_at,
                updated_at
            )
            select workflow_id,
                   workflow_type,
                   aggregate_type,
                   aggregate_id,
                   state,
                   version,
                   correlation_id,
                   failure_reason,
                   last_event_id,
                   payload,
                   created_at,
                   updated_at
            from %I.%I
            on conflict (workflow_type, aggregate_type, aggregate_id) do nothing
        $sql$, current_schema(), 'workflow_states');

        execute format(
            'alter table %I.%I rename to %I',
            current_schema(),
            'workflow_states',
            'workflow_states_legacy_disabled'
        );
    end if;
end $$;

create index if not exists idx_outbox_events_status_next_retry
    on outbox_events (status, next_retry_at, created_at);

create index if not exists idx_outbox_events_correlation_status
    on outbox_events (correlation_id, status);

create index if not exists idx_outbox_events_aggregate_status
    on outbox_events (aggregate_id, status);

create unique index if not exists uq_processed_events_event_consumer
    on processed_events (event_id, consumer_name);

create index if not exists idx_processed_events_failed_retry
    on processed_events (status, next_retry_at, retry_count)
    where status = 'FAILED';

create index if not exists idx_workflow_steps_workflow_id_status
    on workflow_steps (workflow_id, status, started_at);

create index if not exists idx_workflow_instances_workflow_aggregate
    on workflow_instances (workflow_type, aggregate_type, aggregate_id);