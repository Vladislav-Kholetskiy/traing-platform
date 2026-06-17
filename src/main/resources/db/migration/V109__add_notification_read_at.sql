alter table notification
    add column read_at timestamptz null;

create index ix_notif__read_at
    on notification (read_at);
