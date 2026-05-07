-- A function for restoring a deleted case note
create or replace function restore_deleted_case_note(_case_note_id uuid) returns void as
$$
declare
_deleted_id uuid;
begin
select id
into _deleted_id
from case_note_deleted
where case_note_id = _case_note_id
order by deleted_at desc
    limit 1;

insert into case_note(id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                      note_text, created_at, created_by, author_user_id, system_generated, system)
select cn.id,
       cn.person_identifier,
       cn.location_id,
       cn.author_username,
       cn.author_name,
       cn.sub_type_id,
       cn.occurred_at,
       cn.text,
       cn.created_at,
       cn.created_by,
       cn.author_user_id,
       cn.system_generated,
       coalesce(cn.system, 'DPS')
from case_note_deleted cnd
         cross join lateral jsonb_to_record(case_note) as cn(id uuid, person_identifier varchar(12),
                                                                 location_id varchar(12), author_username varchar(64),
                                                                 author_name varchar(80), sub_type_id integer,
                                                                 occurred_at timestamp, text text,
                                                                 created_at timestamp, created_by varchar(64),
                                                                 legacy_id bigint, author_user_id varchar(64),
                                                                 system_generated boolean, system varchar(6))
where cnd.id = _deleted_id;

insert into case_note_amendment(case_note_id, id, author_username, author_name, note_text, created_at, created_by, author_user_id, system)
select _case_note_id,
       cn.id,
       cn.author_username,
       cn.author_name,
       cn.text,
       cn.created_at,
       cn.created_by,
       cn.author_user_id,
       coalesce(cn.system, 'DPS')
from case_note_deleted cnd
         cross join lateral jsonb_to_recordset((case_note ->> 'amendments')::jsonb) as cn(id uuid, case_note_id uuid,
                                                                                              author_username varchar(64),
                                                                                              author_name varchar(80), text text,
                                                                                              created_at timestamp,
                                                                                              created_by varchar(64),
                                                                                              author_user_id varchar(64),
                                                                                              system varchar(6))
where cnd.id = _deleted_id;

end;
$$ language plpgsql;
