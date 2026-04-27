alter table case_note_deleted
    drop column id,
    add column id uuid not null default gen_random_uuid() primary key;

drop sequence if exists case_note_deleted_id_seq;

alter table case_note
    drop column version;

alter table case_note_amendment
    drop column version;
