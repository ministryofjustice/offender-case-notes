create or replace function hard_delete_soft_deleted() returns void as
$$
declare
    case_note_ids uuid[];
begin
    select array_agg(id) into case_note_ids from case_note where soft_deleted = true;

    with case_notes as (select cn.*, json_agg(row_to_json(cna.*)) as amendments
                        from case_note cn
                                 left join case_note_amendment cna on cna.case_note_id = cn.id
                        where cn.id = any (case_note_ids)
                        group by cn.id)
    insert
    into case_note_deleted(id, person_identifier, case_note_id, legacy_id, case_note, deleted_at, deleted_by,
                           caseload_id, source, cause)
    select nextval('case_note_deleted_id_seq'),
           cn.person_identifier,
           cn.id,
           cn.legacy_id,
           row_to_json(cn.*),
           cn.modify_date_time,
           cn.modify_user_id,
           null,
           'DPS',
           'DELETE'
    from case_notes cn
    where not exists(select 1 from case_note_deleted where case_note_id = cn.id);

    delete from case_note_amendment where case_note_id = any (case_note_ids);
    delete from case_note where id = any (case_note_ids);
end ;
$$ language plpgsql;

select hard_delete_soft_deleted();