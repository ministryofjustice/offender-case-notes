alter table offender_case_note_amendment
    add SOFT_DELETED BOOLEAN default FALSE;

alter table offender_case_note
    add SOFT_DELETED BOOLEAN default FALSE;
