alter table outbox_events
    add column if not exists idempotency_key varchar(128);

create unique index if not exists uq_outbox_events_idempotency_key
    on outbox_events (idempotency_key)
    where idempotency_key is not null;

create unique index if not exists uq_kitchen_ticket_items_order_item
    on kitchen_ticket_items (order_item_id);

create unique index if not exists uq_low_stock_alerts_one_open_per_item
    on low_stock_alerts (inventory_item_id)
    where status = 'open';

create unique index if not exists uq_stock_transactions_source_once
    on stock_transactions (inventory_item_id, source_ref, source_type, reason)
    where source_ref is not null
      and source_type is not null;

create unique index if not exists uq_payments_gateway_ref
    on payments (gateway_ref)
    where gateway_ref is not null;

create unique index if not exists uq_refunds_external_transaction_ref
    on refunds (external_transaction_ref)
    where external_transaction_ref is not null;
