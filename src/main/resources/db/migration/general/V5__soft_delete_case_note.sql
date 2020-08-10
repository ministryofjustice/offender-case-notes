alter table offender_case_note_amendment
    add deleted BOOLEAN default FALSE;

alter table offender_case_note
    add deleted BOOLEAN default FALSE;
