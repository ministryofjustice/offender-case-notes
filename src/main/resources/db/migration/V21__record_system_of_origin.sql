alter table case_note_deleted
    rename source to system;

alter table case_note
    add column system varchar(6) not null default 'DPS'
        constraint check_system check (system in ('DPS', 'NOMIS'));

alter table case_note_amendment
    add column system varchar(6) not null default 'DPS'
        constraint check_system check (system in ('DPS', 'NOMIS'));