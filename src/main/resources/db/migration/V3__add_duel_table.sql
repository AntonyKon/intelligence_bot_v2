create table duels
(
    id              identity primary key not null,
    attacking_user  bigint               not null,
    defending_user  bigint,
    victorious_user bigint,
    group_id        bigint               not null,
    duel_date       date                 not null,
    constraint fk_duels_attacking_user
        foreign key (attacking_user) references GROUP_USERS,
    constraint fk_duels_defending_user
        foreign key (defending_user) references GROUP_USERS,
    constraint fk_duels_victorious_user
        foreign key (victorious_user) references GROUP_USERS
);