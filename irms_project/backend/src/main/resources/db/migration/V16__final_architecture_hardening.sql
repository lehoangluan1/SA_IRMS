-- Final source hardening: durable workflow instances/steps, consumer traceability, notification delivery attempts, and projection indexes.

create table if not exists workflow_instances (
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

create index if not exists idx_workflow_instances_state
    on workflow_instances (workflow_type, state, updated_at);

create index if not exists idx_workflow_instances_correlation
    on workflow_instances (correlation_id);

create index if not exists idx_workflow_instances_aggregate
    on workflow_instances (aggregate_type, aggregate_id);

create table if not exists workflow_steps (
    workflow_step_id uuid primary key,
    workflow_id uuid not null references workflow_instances(workflow_id) on delete cascade,
    workflow_type varchar(120) not null,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    step_name varchar(160) not null,
    status varchar(40) not null,
    event_id uuid,
    attempt integer not null default 1,
    error varchar(1000),
    payload jsonb not null default '{}'::jsonb,
    started_at timestamptz not null default now(),
    completed_at timestamptz,
    check (status in ('STARTED', 'COMPLETED', 'FAILED', 'SKIPPED', 'COMPENSATED'))
);

create index if not exists idx_workflow_steps_workflow
    on workflow_steps (workflow_id, started_at);

create index if not exists idx_workflow_steps_event
    on workflow_steps (event_id);

create index if not exists idx_workflow_steps_aggregate
    on workflow_steps (aggregate_type, aggregate_id);

alter table processed_events
    add column if not exists correlation_id varchar(100),
    add column if not exists causation_id varchar(100),
    add column if not exists retry_count integer not null default 0,
    add column if not exists next_retry_at timestamptz;

create unique index if not exists uq_processed_event_consumer
    on processed_events (event_id, consumer_name);

create index if not exists idx_processed_events_retry
    on processed_events (status, next_retry_at)
    where status = 'FAILED';

create index if not exists idx_processed_events_correlation
    on processed_events (correlation_id);

alter table dead_letter_events
    add column if not exists correlation_id varchar(100),
    add column if not exists causation_id varchar(100),
    add column if not exists replay_count integer not null default 0;

create index if not exists idx_dead_letter_events_status
    on dead_letter_events (status, created_at);

create index if not exists idx_dead_letter_events_correlation
    on dead_letter_events (correlation_id);

create table if not exists notification_deliveries (
    delivery_id uuid primary key,
    message_id uuid not null references notification_messages(message_id) on delete cascade,
    channel varchar(30) not null,
    delivery_status varchar(40) not null,
    attempt integer not null default 1,
    provider_message_id varchar(180),
    failure_reason varchar(1000),
    next_retry_at timestamptz,
    delivered_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (delivery_status in ('QUEUED', 'SENT', 'FAILED', 'RETRY_SCHEDULED', 'DLQ'))
);

create index if not exists idx_notification_deliveries_message
    on notification_deliveries (message_id, attempt);

create index if not exists idx_notification_deliveries_retry
    on notification_deliveries (delivery_status, next_retry_at);

create table if not exists reporting_sales_projection (
    projection_id uuid primary key,
    event_id uuid not null unique,
    bill_id uuid,
    payment_id uuid,
    amount numeric(12,2) not null default 0,
    occurred_at timestamptz not null,
    projected_at timestamptz not null default now()
);

create table if not exists reporting_kitchen_delay_projection (
    projection_id uuid primary key,
    event_id uuid not null unique,
    ticket_id uuid,
    order_id uuid,
    station_code varchar(80),
    status varchar(80),
    deadline_at timestamptz,
    occurred_at timestamptz not null,
    projected_at timestamptz not null default now()
);

create index if not exists idx_outbox_events_correlation_id
    on outbox_events (correlation_id);

create index if not exists idx_outbox_events_aggregate_id
    on outbox_events (aggregate_id);

create index if not exists idx_outbox_events_retry
    on outbox_events (status, next_retry_at)
    where status in ('PENDING', 'FAILED');

create unique index if not exists uq_outbox_events_idempotency_key
    on outbox_events (idempotency_key)
    where idempotency_key is not null;
