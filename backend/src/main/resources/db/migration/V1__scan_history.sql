create table scans (
 id uuid primary key,
 project_name varchar(100) not null,
 created_at timestamp with time zone not null,
 summary text not null,
 report text not null
);
create index idx_scans_created on scans(created_at desc);
