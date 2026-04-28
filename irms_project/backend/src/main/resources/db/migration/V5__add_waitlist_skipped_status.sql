do $$
declare
    constraint_name text;
begin
    select conname
    into constraint_name
    from pg_constraint
    where conrelid = 'waitlist_entries'::regclass
      and contype = 'c'
      and pg_get_constraintdef(oid) like '%status in (%waiting%, %notified%, %seated%, %left%, %expired%';

    if constraint_name is not null then
        execute format('alter table waitlist_entries drop constraint %I', constraint_name);
    end if;
end
$$;

alter table waitlist_entries
    add constraint chk_waitlist_entries_status
    check (status in ('waiting', 'notified', 'skipped', 'seated', 'left', 'expired'));
