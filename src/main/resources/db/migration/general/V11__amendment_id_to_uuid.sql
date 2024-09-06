alter table offender_case_note_amendment
    drop column offender_case_note_amendment_id,
    add column offender_case_note_amendment_id uuid not null default gen_random_uuid(),
    add column version                         int;

alter table offender_case_note
    add column version int;