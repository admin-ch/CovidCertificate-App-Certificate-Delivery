alter table t_transfer add column public_key_sha_256 character varying(64);
update t_transfer set public_key_sha_256 = encode(sha256(public_key::bytea), 'base64');
alter table t_transfer alter column public_key_sha_256 set not null;
create index idx_public_key_sha_256 on t_transfer ( public_key_sha_256 );