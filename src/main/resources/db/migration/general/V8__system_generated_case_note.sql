alter table offender_case_note
    add column system_generated boolean not null default false;
