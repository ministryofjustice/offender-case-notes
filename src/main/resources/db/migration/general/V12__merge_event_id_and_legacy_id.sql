alter table offender_case_note
    drop column legacy_id;

alter table offender_case_note
    rename column event_id to legacy_id;

alter table offender_case_note
    add constraint unq_legacy_id unique (legacy_id);