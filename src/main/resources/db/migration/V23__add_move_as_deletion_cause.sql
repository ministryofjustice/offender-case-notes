alter table case_note_deleted
    drop constraint check_cause;

alter table case_note_deleted
    add constraint check_cause check (cause in ('DELETE', 'UPDATE', 'MERGE', 'MOVE'))