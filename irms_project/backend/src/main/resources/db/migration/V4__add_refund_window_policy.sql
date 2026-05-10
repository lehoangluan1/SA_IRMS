alter table authorization_policies
    add column if not exists refund_window_hours integer not null default 24;

alter table authorization_policies
    add constraint chk_authorization_policies_refund_window_hours
    check (refund_window_hours > 0);

update authorization_policies
set refund_window_hours = 24
where refund_window_hours is null or refund_window_hours <= 0;
