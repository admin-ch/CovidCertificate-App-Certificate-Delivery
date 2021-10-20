alter table t_transfer add column expires_at timestamp with time zone;
update t_transfer set expires_at=t_transfer.created_at + interval '30 day' WHERE expires_at is null;
alter table t_transfer alter column expires_at set not null;

alter table t_transfer add column fails_at timestamp with time zone;
update t_transfer set fails_at=t_transfer.created_at + interval '33 day' WHERE fails_at is null;
alter table t_transfer alter column fails_at set not null;
