alter table offender_case_note
    rename column offender_case_note_id to id;

alter table offender_case_note
    rename column offender_identifier to person_identifier;

alter table offender_case_note
    rename column case_note_type_id to type_id;

alter table offender_case_note
    rename column occurrence_date_time to occurred_at;

alter table offender_case_note
    rename column create_date_time to created_at;

alter table offender_case_note
    rename column create_user_id to created_by;

alter table offender_case_note_amendment
    rename column offender_case_note_amendment_id to id;

alter table offender_case_note_amendment
    rename column offender_case_note_id to case_note_id;

alter table offender_case_note_amendment
    rename column create_date_time to created_at;

alter table offender_case_note_amendment
    rename column create_user_id to created_by;

alter sequence offender_case_note_event_id_seq rename to case_note_legacy_id_seq;

alter table offender_case_note
    rename to case_note;

alter table offender_case_note_amendment
    rename to case_note_amendment;

create table if not exists case_note_deleted
(
    id                bigserial   not null primary key,
    person_identifier varchar     not null,
    case_note_id      uuid        not null,
    legacy_id         bigint      not null,
    case_note         jsonb       not null,
    deleted_datetime  timestamp   not null,
    deleted_username  varchar(64) not null,
    caseload_id       varchar(12),
    source            varchar(6)
        constraint check_source check (source in ('DPS', 'NOMIS')),
    cause             varchar(6)
        constraint check_cause check (cause in ('DELETE', 'UPDATE', 'MERGE'))
);

create index case_note_deleted_person_identifier on case_note_deleted (person_identifier);
create index case_note_deleted_case_note_id on case_note_deleted (caseload_id);
create index case_note_deleted_legacy_id on case_note_deleted (legacy_id);
create index case_note_deleted_cause on case_note_deleted (cause);

create or replace function hard_delete_soft_deleted() returns void as
$$
declare
    case_note_ids uuid[];
begin
    select array_agg(id) into case_note_ids from case_note where soft_deleted = true;

    with case_notes as (select cn.*, json_agg(row_to_json(cna.*)) as amendments
                        from case_note cn
                                 join case_note_amendment cna on cna.case_note_id = cn.id
                        where cn.id = any (case_note_ids)
                        group by cn.id)
    insert
    into case_note_deleted(id, person_identifier, case_note_id, legacy_id, case_note, deleted_datetime,
                           deleted_username, caseload_id, source, cause)
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
    from case_notes cn;
end ;
$$ language plpgsql;

select hard_delete_soft_deleted();

alter table case_note
    drop column modify_date_time,
    drop column modify_user_id;

alter table case_note_amendment
    drop column modify_date_time,
    drop column modify_user_id;