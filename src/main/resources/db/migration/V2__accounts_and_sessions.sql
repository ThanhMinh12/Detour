create table app_users (
    id uuid primary key,
    email varchar(320) not null unique,
    display_name varchar(100) not null,
    password_hash varchar(255) not null,
    created_at timestamp(6) with time zone not null
);

alter table trip_members add column user_id uuid;
alter table trip_members add constraint fk_trip_members_user foreign key (user_id) references app_users(id);
create unique index uk_trip_members_trip_user on trip_members(trip_id, user_id);
create index ix_trip_members_user on trip_members(user_id);

create table spring_session (
    primary_id char(36) not null,
    session_id char(36) not null,
    creation_time bigint not null,
    last_access_time bigint not null,
    max_inactive_interval integer not null,
    expiry_time bigint not null,
    principal_name varchar(320),
    constraint spring_session_pk primary key (primary_id)
);
create unique index spring_session_ix1 on spring_session(session_id);
create index spring_session_ix2 on spring_session(expiry_time);
create index spring_session_ix3 on spring_session(principal_name);

create table spring_session_attributes (
    session_primary_id char(36) not null,
    attribute_name varchar(200) not null,
    attribute_bytes bytea not null,
    constraint spring_session_attributes_pk primary key (session_primary_id, attribute_name),
    constraint spring_session_attributes_fk foreign key (session_primary_id)
        references spring_session(primary_id) on delete cascade
);
