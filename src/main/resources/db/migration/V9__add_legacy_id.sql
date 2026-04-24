alter table offender_case_note
    add column legacy_id bigint unique nulls distinct;