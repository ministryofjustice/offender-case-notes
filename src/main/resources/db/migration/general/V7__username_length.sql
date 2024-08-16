alter table case_note_parent_type
    alter column create_user_id type varchar(64),
    alter column modify_user_id type varchar(64)
;

alter table case_note_type
    alter column create_user_id type varchar(64),
    alter column modify_user_id type varchar(64)
;

alter table offender_case_note
    alter column create_user_id type varchar(64),
    alter column modify_user_id type varchar(64),
    alter column author_user_id type varchar(64),
    alter column author_username type varchar(64)
;

alter table offender_case_note_amendment
    alter column create_user_id type varchar(64),
    alter column modify_user_id type varchar(64),
    alter column author_user_id type varchar(64),
    alter column author_username type varchar(64)
;