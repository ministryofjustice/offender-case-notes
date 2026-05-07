drop function hard_delete_soft_deleted();

alter table case_note
    drop column soft_deleted,
    drop column modify_date_time,
    drop column modify_user_id;

alter table case_note_amendment
    drop column soft_deleted,
    drop column modify_date_time,
    drop column modify_user_id;