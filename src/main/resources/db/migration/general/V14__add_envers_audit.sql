create table audit_revision
(
    id                bigserial    not null primary key,
    timestamp         timestamp    not null,
    username          varchar(32)  not null,
    user_display_name varchar(255) not null,
    caseload_id       varchar(10),
    source            varchar(6)
        constraint check_source check (source in ('DPS', 'NOMIS'))
);

create table if not exists offender_case_note_audit
(
    rev_id                bigint      not null references audit_revision (id),
    rev_type              smallint    not null,
    offender_case_note_id uuid        not null,
    offender_identifier   varchar(12) not null,
    location_id           varchar(12) not null,
    author_username       varchar(32) not null,
    author_name           varchar(80) not null,
    case_note_type_id     integer     not null,
    occurrence_date_time  timestamp   not null,
    note_text             text        not null,
    create_date_time      timestamp   not null,
    create_user_id        varchar(32) not null,
    modify_date_time      timestamp,
    modify_user_id        varchar(32),
    legacy_id             bigint      not null,
    author_user_id        varchar(36) not null,
    system_generated      boolean     not null,

    text_modified         boolean     not null,
    type_modified         boolean     not null,

    primary key (rev_id, offender_case_note_id)
);

create table if not exists offender_case_note_amendment_audit
(
    rev_id                          bigint      not null references audit_revision (id),
    rev_type                        smallint    not null,
    offender_case_note_amendment_id uuid        not null,
    offender_case_note_id           uuid        not null,
    author_username                 varchar(64) not null,
    author_name                     varchar(80) not null,
    note_text                       text        not null,
    create_date_time                timestamp   not null,
    create_user_id                  varchar(64) not null,
    modify_date_time                timestamp,
    modify_user_id                  varchar(64),
    author_user_id                  varchar(64) not null,

    text_modified                   boolean     not null,

    primary key (rev_id, offender_case_note_amendment_id)
);