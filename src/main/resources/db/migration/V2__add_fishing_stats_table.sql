create table fishing_stats
(
    id            identity primary key not null,
    user_id       bigint               not null,
    catch_amount   double               not null,
    last_catch_time datetime             not null,
    constraint fk_fishing_stats_user_id
        foreign key (user_id) references group_users
);