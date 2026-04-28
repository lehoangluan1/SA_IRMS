-- Adds RabbitMQ-ready transactional outbox, inbox/idempotency, workflow state, DLQ, and replay metadata.

alter table outbox_events
    add column if not exists id bigserial,
    add column if not exists event_type varchar(120),
    add column if not exists event_version integer not null default 1,
    add column if not exists producer_service varchar(120),
    add column if not exists routing_key varchar(160),
    add column if not exists exchange_name varchar(160),
    add column if not exists causation_id varchar(100),
    add column if not exists next_retry_at timestamptz,
    add column if not exists published_at timestamptz;

update outbox_events
set event_type = coalesce(event_type, type),
    producer_service = coalesce(producer_service, 'irms-legacy'),
    causation_id = coalesce(causation_id, correlation_id),
    next_retry_at = coalesce(next_retry_at, occurred_at)
where event_type is null
   or producer_service is null
   or causation_id is null
   or next_retry_at is null;

alter table outbox_events
    drop constraint if exists outbox_events_status_check;

alter table outbox_events
    add constraint outbox_events_status_check
        check (status in ('pending', 'processed', 'failed', 'PENDING', 'PUBLISHED', 'FAILED'));

create index if not exists idx_outbox_events_publish_pending
    on outbox_events (status, next_retry_at, occurred_at)
    where status = 'PENDING';

create index if not exists idx_outbox_events_routing
    on outbox_events (exchange_name, routing_key)
    where exchange_name is not null and routing_key is not null;

create table if not exists processed_events (
    event_id uuid not null,
    consumer_name varchar(180) not null,
    event_type varchar(120) not null,
    processed_at timestamptz not null default now(),
    status varchar(30) not null,
    error varchar(1000),
    primary key (event_id, consumer_name),
    check (status in ('IN_PROGRESS', 'PROCESSED', 'FAILED'))
);

create index if not exists idx_processed_events_consumer
    on processed_events (consumer_name, processed_at desc);

create table if not exists workflow_states (
    workflow_id uuid primary key,
    workflow_type varchar(120) not null,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    state varchar(80) not null,
    version integer not null default 0,
    correlation_id varchar(100) not null,
    failure_reason varchar(1000),
    last_event_id uuid,
    payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (workflow_type, aggregate_type, aggregate_id)
);

create index if not exists idx_workflow_states_state
    on workflow_states (workflow_type, state, updated_at);

create table if not exists dead_letter_events (
    dead_letter_id uuid primary key,
    event_id uuid,
    consumer_name varchar(180) not null,
    event_type varchar(120) not null,
    payload jsonb not null default '{}'::jsonb,
    broker_headers jsonb not null default '{}'::jsonb,
    failure_reason varchar(1000) not null,
    status varchar(30) not null default 'FAILED',
    replayed_at timestamptz,
    created_at timestamptz not null default now(),
    check (status in ('FAILED', 'REPLAY_REQUESTED', 'REPLAYED'))
);

create index if not exists idx_dead_letter_events_event
    on dead_letter_events (event_id, consumer_name);

create table if not exists event_replay_requests (
    replay_id uuid primary key,
    event_id uuid not null,
    consumer_name varchar(180),
    reason varchar(1000),
    status varchar(30) not null default 'QUEUED',
    requested_at timestamptz not null default now(),
    completed_at timestamptz,
    check (status in ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED'))
);

create table if not exists reporting_event_projection (
    event_id uuid primary key,
    event_type varchar(120) not null,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    payload jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null,
    projected_at timestamptz not null default now()
);

alter table audit_logs
    alter column actor_user_id drop not null;
