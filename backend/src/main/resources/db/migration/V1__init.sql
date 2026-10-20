create table users (
    id bigserial primary key,
    username varchar(30) not null unique,
    password_hash varchar(100) not null
);

create table event (
    id bigserial primary key,
    sport varchar(20) not null,
    external_id varchar(60) not null,
    name varchar(120) not null,
    starts_at timestamptz not null,
    status varchar(20) not null,
    unique (sport, external_id)
);

create table athlete (
    id bigserial primary key,
    sport varchar(20) not null,
    external_id varchar(60) not null,
    name varchar(120) not null,
    team varchar(120),
    kind varchar(20) not null,
    image_url varchar(300),
    price double precision not null,
    unique (sport, external_id)
);

create table athlete_score (
    id bigserial primary key,
    event_id bigint not null references event (id),
    athlete_id bigint not null references athlete (id),
    points double precision not null,
    breakdown varchar(2000),
    unique (event_id, athlete_id)
);

create table league (
    id bigserial primary key,
    name varchar(60) not null,
    sport varchar(20) not null,
    invite_code varchar(12) not null unique,
    owner_id bigint not null references users (id)
);

create table league_member (
    id bigserial primary key,
    league_id bigint not null references league (id),
    user_id bigint not null references users (id),
    unique (league_id, user_id)
);

create table lineup (
    id bigserial primary key,
    user_id bigint not null references users (id),
    event_id bigint not null references event (id),
    total_points double precision not null default 0,
    unique (user_id, event_id)
);

create table lineup_pick (
    lineup_id bigint not null references lineup (id) on delete cascade,
    athlete_id bigint not null references athlete (id),
    primary key (lineup_id, athlete_id)
);

create index idx_event_sport_starts on event (sport, starts_at);
create index idx_lineup_event on lineup (event_id);
