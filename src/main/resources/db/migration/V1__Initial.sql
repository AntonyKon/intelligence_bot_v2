create table group_users
(
    id identity not null primary key,
    telegram_id bigint           not null,
    group_id    bigint           not null,
    is_admin boolean not null,
    money       double precision not null
);

create table handsome_fag_flags
(
    group_id identity not null primary key,
    fag_user           bigint,
    handsome_user      bigint,
    fag_flag_date      date,
    handsome_flag_date date,
    constraint FK_HANDSOME_FAG_FLAGS_FAG_USER__ID
        foreign key (fag_user) references group_users,
    constraint FK_HANDSOME_FAG_FLAGS_HANDSOME_FLAG_USER__ID
        foreign key (handsome_user) references group_users
);

create table handsome_fag_stats
(
    id identity not null primary key,
    group_user_id  bigint not null,
    fag_count      bigint not null default 0,
    handsome_count bigint not null default 0,
    constraint fk_hansome_fag_stats_group_user_id
        foreign key (group_user_id) references group_users
);

create table feature_toggle
(
    id identity not null primary key,
    group_id      bigint       not null,
    feature       varchar(255) not null,
    feature_value varchar(255) not null
);