alter table kitchen_tickets
    drop constraint if exists kitchen_tickets_status_check;

alter table kitchen_ticket_items
    drop constraint if exists kitchen_ticket_items_status_check;

alter table dish_status_events
    drop constraint if exists dish_status_events_type_check;

update dish_status_events
set type = 'cooking'
where type = 'started';

update kitchen_ticket_items
set status = case status
    when 'new' then 'queued'
    when 'started' then 'cooking'
    else status
end
where status in ('new', 'started');

update kitchen_tickets
set status = case status
    when 'new' then 'queued'
    when 'started' then 'cooking'
    else status
end
where status in ('new', 'started');

alter table kitchen_ticket_items
    add column if not exists next_action_at timestamptz;

alter table kitchen_ticket_items
    add column if not exists cooking_started_at timestamptz;

alter table kitchen_ticket_items
    add column if not exists inventory_deducted_at timestamptz;

alter table kitchen_tickets
    add column if not exists next_action_at timestamptz;

update kitchen_ticket_items
set next_action_at = case
        when next_action_at is not null then next_action_at
        when status = 'queued' then coalesce(fire_at, created_at, now())
        when status = 'cooking' then now() + interval '30 seconds'
        else null
    end,
    cooking_started_at = case
        when cooking_started_at is not null then cooking_started_at
        when status in ('cooking', 'ready', 'served') then updated_at
        else null
    end
where next_action_at is null
   or cooking_started_at is null;

alter table kitchen_tickets
    add constraint kitchen_tickets_status_check
    check (status in ('queued', 'cooking', 'ready', 'blocked', 'hold_for_service', 'served'));

alter table kitchen_ticket_items
    add constraint kitchen_ticket_items_status_check
    check (status in ('queued', 'cooking', 'ready', 'blocked', 'hold_for_service', 'served'));

alter table dish_status_events
    add constraint dish_status_events_type_check
    check (type in ('queued', 'cooking', 'ready', 'blocked', 'served', 'returned'));

create index if not exists idx_kitchen_ticket_items_status_next_action
    on kitchen_ticket_items (status, next_action_at);

create index if not exists idx_kitchen_tickets_status_next_action
    on kitchen_tickets (status, next_action_at);

create unique index if not exists uq_stock_transactions_order_item_cooking_start
    on stock_transactions (inventory_item_id, source_ref, source_type, reason)
    where source_type = 'order_item_cooking_start'
      and source_ref is not null
      and reason = 'consumption';
