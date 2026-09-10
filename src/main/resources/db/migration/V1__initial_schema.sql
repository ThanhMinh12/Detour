create table trips (
    id uuid primary key,
    name varchar(255) not null,
    destination varchar(255) not null,
    start_date date not null,
    end_date date not null,
    currency varchar(3) not null,
    invite_code varchar(12) not null unique,
    created_at timestamp(6) with time zone not null,
    constraint ck_trips_date_range check (end_date >= start_date)
);

create table trip_members (
    id uuid primary key,
    trip_id uuid not null references trips(id),
    display_name varchar(255) not null,
    email varchar(255) not null,
    role varchar(255) not null,
    joined_at timestamp(6) with time zone not null,
    constraint uk_trip_members_trip_email unique (trip_id, email),
    constraint ck_trip_members_role check (role in ('ORGANIZER', 'MEMBER'))
);

create table activities (
    id uuid primary key,
    trip_id uuid not null references trips(id),
    proposed_by_member_id uuid,
    date date not null,
    start_time time(6),
    type varchar(255) not null,
    status varchar(255) not null,
    title varchar(255) not null,
    place varchar(255),
    notes varchar(2000),
    reservation_reference varchar(255),
    booking_url varchar(255),
    constraint fk_activities_proposer foreign key (proposed_by_member_id) references trip_members(id),
    constraint ck_activities_type check (type in ('ACTIVITY', 'FOOD', 'LODGING', 'TRANSIT')),
    constraint ck_activities_status check (status in ('PROPOSED', 'CONFIRMED', 'CANCELLED'))
);

create table activity_votes (
    id uuid primary key,
    activity_id uuid not null references activities(id) on delete cascade,
    member_id uuid not null references trip_members(id),
    vote_value integer not null,
    constraint uk_activity_votes_activity_member unique (activity_id, member_id),
    constraint ck_activity_votes_value check (vote_value between -1 and 1)
);

create table expenses (
    id uuid primary key,
    trip_id uuid not null references trips(id),
    description varchar(255) not null,
    paid_by_member_id uuid not null references trip_members(id),
    category varchar(255) not null,
    split_mode varchar(255) not null,
    occurred_on date not null,
    subtotal_cents bigint not null,
    tax_cents bigint not null,
    tip_cents bigint not null,
    total_cents bigint not null,
    created_at timestamp(6) with time zone not null,
    constraint ck_expenses_category check (category in ('FOOD', 'LODGING', 'TRANSIT', 'ACTIVITY', 'SHOPPING', 'OTHER')),
    constraint ck_expenses_split_mode check (split_mode in ('EQUAL', 'EXACT', 'PERCENTAGE', 'ITEMIZED')),
    constraint ck_expenses_amounts check (
        subtotal_cents > 0 and tax_cents >= 0 and tip_cents >= 0
        and total_cents = subtotal_cents + tax_cents + tip_cents
    )
);

create table expense_shares (
    id uuid primary key,
    expense_id uuid not null references expenses(id) on delete cascade,
    member_id uuid not null references trip_members(id),
    subtotal_cents bigint not null,
    tax_cents bigint not null,
    tip_cents bigint not null,
    total_cents bigint not null,
    constraint uk_expense_shares_expense_member unique (expense_id, member_id),
    constraint ck_expense_shares_amounts check (
        subtotal_cents >= 0 and tax_cents >= 0 and tip_cents >= 0
        and total_cents = subtotal_cents + tax_cents + tip_cents
    )
);

create table reimbursements (
    id uuid primary key,
    trip_id uuid not null references trips(id),
    from_member_id uuid not null references trip_members(id),
    to_member_id uuid not null references trip_members(id),
    amount_cents bigint not null,
    note varchar(500),
    paid_on date not null,
    created_at timestamp(6) with time zone not null,
    constraint ck_reimbursements_members check (from_member_id <> to_member_id),
    constraint ck_reimbursements_amount check (amount_cents > 0)
);

create index ix_trip_members_trip_joined on trip_members(trip_id, joined_at);
create index ix_activities_trip_date_time on activities(trip_id, date, start_time);
create index ix_activity_votes_activity on activity_votes(activity_id);
create index ix_expenses_trip_date_created on expenses(trip_id, occurred_on, created_at);
create index ix_reimbursements_trip_date_created on reimbursements(trip_id, paid_on, created_at);
