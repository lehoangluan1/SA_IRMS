-- DEPRECATED COMPATIBILITY ARTIFACT.
-- This table is retained only for backward-compatible history and is not part of the canonical RabbitMQ outbox relay pipeline.
-- The active broker fanout path uses outbox_events + inbox/processed-events with RabbitMQ relay and consumers.

create table if not exists outbox_event_deliveries (
    event_id uuid not null references outbox_events(event_id) on delete cascade,
    consumer_name varchar(160) not null,
    status varchar(30) not null default 'pending',
    retry_count integer not null default 0,
    processed_at timestamptz,
    last_error varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (event_id, consumer_name),
    check (status in ('pending', 'processed', 'failed'))
);

create index if not exists idx_outbox_event_deliveries_pending
    on outbox_event_deliveries (consumer_name, created_at)
    where status = 'pending';

create index if not exists idx_outbox_event_deliveries_event
    on outbox_event_deliveries (event_id, status);

insert into outbox_event_deliveries (
    event_id,
    consumer_name,
    status,
    retry_count,
    processed_at,
    last_error,
    created_at,
    updated_at
)
select event_id,
       consumer_name,
       status,
       retry_count,
       processed_at,
       last_error,
       created_at,
       now()
from (
    select e.*, 'notification.notification-request.delivery'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'NotificationRequested'

    union all

    select e.*, 'identity.audit-recording.materializer'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'AuditRecordingRequested'

    union all

    select e.*, 'notification.audit-follow-up.router'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'AuditFollowUpRequested'

    union all

    select e.*, 'identity.session-touch.materializer'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'SessionTouchRequested'

    union all

    select e.*, 'inventory.low-stock.evaluator'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'InventoryStockChanged'

    union all

    select e.*, 'reporting.inventory.projection'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'InventoryStockChanged'

    union all

    select e.*, 'kitchen.dish-status.persistence'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'KitchenDishStatusChanged'

    union all

    select e.*, 'reporting.kitchen.projection'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'KitchenDishStatusChanged'

    union all

    select e.*, 'common.legacy.reservation-confirmed.ack'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'ReservationConfirmed'

    union all

    select e.*, 'common.legacy.inventory-low.ack'::varchar(160) as consumer_name
    from outbox_events e
    where e.type = 'InventoryLow'
) planned
on conflict (event_id, consumer_name) do nothing;
