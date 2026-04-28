alter table notification_messages
    add column waitlist_entry_id uuid references waitlist_entries(waitlist_entry_id);

create index idx_notifications_waitlist_entry on notification_messages(waitlist_entry_id);
