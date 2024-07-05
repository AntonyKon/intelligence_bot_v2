create table daily_events(
    id identity primary key not null,
    group_id bigint not null,
    last_event_date date
);