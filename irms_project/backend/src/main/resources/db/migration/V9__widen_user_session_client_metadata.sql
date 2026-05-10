alter table user_sessions
    alter column device_id type varchar(512),
    alter column ip_address type varchar(150);
