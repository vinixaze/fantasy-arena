alter table users add column role varchar(20) not null default 'USER';

create table admin_audit (
    id bigserial primary key,
    admin_id bigint not null references users (id),
    action varchar(80) not null,
    entity_type varchar(40) not null,
    entity_id varchar(80),
    details varchar(2000),
    created_at timestamptz not null default now()
);

create index idx_admin_audit_created on admin_audit (created_at desc);
