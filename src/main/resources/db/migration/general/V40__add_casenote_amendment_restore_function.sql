-- A function for restoring a deleted case note amendment to the original case note.
-- _deleted_case_note_id refers to the ID column on case_note_deleted NOT the ID of the deleted case note
-- This cannot be run for amendments unless the associated case note already exists
create or replace function restore_deleted_amendment(_deleted_case_note_id uuid, _amendment_id uuid)
       returns void
as
$$
begin

insert into case_note_amendment(case_note_id, id, author_username, author_name, note_text, created_at, created_by, author_user_id, system)
select cnd.case_note_id,
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
where cnd.id = _deleted_case_note_id and cn.id = _amendment_id;

end;
$$ language plpgsql;
